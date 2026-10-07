package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.carta.CartaBebida;
import com.hafood.sistema.domain.carta.CartaCategoria;
import com.hafood.sistema.domain.carta.CartaPlato;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CartaDTO;
import com.hafood.sistema.dto.CartaMenuDTO;
import com.hafood.sistema.mapper.CartaMapper;
import com.hafood.sistema.repository.CartaBebidaRepository;
import com.hafood.sistema.repository.CartaCategoriaRepository;
import com.hafood.sistema.repository.CartaPlatoRepository;
import com.hafood.sistema.repository.CartaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartaMenuService {

    private final CartaRepository cartaRepository;
    private final CartaCategoriaRepository cartaCategoriaRepository;
    private final CartaPlatoRepository cartaPlatoRepository;
    private final CartaBebidaRepository cartaBebidaRepository;
    private final SedeAccesoService sedeAccesoService;

    public record ProductoCarta(Long productoId, String nombre, BigDecimal precio, boolean agotado) {
    }

    private record Item(Categoria categoria, TipoCategoria tipo, int orden, CartaMenuDTO.Producto producto) {
    }

    @Transactional(readOnly = true)
    public List<CartaDTO> listarActivas(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        return cartaRepository.listarPorSede(sedeId).stream()
                .filter(Carta::isActivo)
                .map(CartaMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CartaMenuDTO obtener(Long cartaId, Usuario actor) {
        Carta carta = cartaRepository.findById(cartaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La carta no existe"));
        sedeAccesoService.exigirAcceso(actor, carta.getSede().getId());

        if (!carta.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carta está inactiva");
        }

        List<CartaCategoria> categorias = cartaCategoriaRepository.findVisibles(cartaId);
        Set<Long> habilitadas = categorias.stream().map(f -> f.getCategoria().getId()).collect(Collectors.toSet());

        List<Item> items = new ArrayList<>();
        cartaPlatoRepository.findVisibles(cartaId).forEach(f -> items.add(new Item(
                f.getPlato().getCategoria(), TipoCategoria.PLATO, f.getOrden(),
                new CartaMenuDTO.Producto(f.getPlato().getId(), TipoCategoria.PLATO, f.getPlato().getNombre(),
                        f.getPlato().getDescripcion(), precioDe(f.getPrecio(), f.getPlato().getPrecioVenta()),
                        f.isAgotado()))));
        cartaBebidaRepository.findVisibles(cartaId).forEach(f -> items.add(new Item(
                f.getBebida().getCategoria(), TipoCategoria.BEBIDA, f.getOrden(),
                new CartaMenuDTO.Producto(f.getBebida().getId(), TipoCategoria.BEBIDA, f.getBebida().getNombre(),
                        f.getBebida().getDescripcion(), precioDe(f.getPrecio(), f.getBebida().getPrecioVenta()),
                        f.isAgotado()))));

        items.sort(Comparator.comparingInt(Item::orden)
                .thenComparing(i -> i.producto().nombre(), String.CASE_INSENSITIVE_ORDER));

        Map<Long, List<CartaMenuDTO.Producto>> porCategoria = new HashMap<>();
        List<CartaMenuDTO.Producto> otrosPlatos = new ArrayList<>();
        List<CartaMenuDTO.Producto> otrasBebidas = new ArrayList<>();

        for (Item item : items) {
            if (item.categoria() == null) {
                (item.tipo() == TipoCategoria.PLATO ? otrosPlatos : otrasBebidas).add(item.producto());
            } else if (habilitadas.contains(item.categoria().getId())) {
                porCategoria.computeIfAbsent(item.categoria().getId(), k -> new ArrayList<>()).add(item.producto());
            }
        }

        List<CartaMenuDTO.Grupo> grupos = new ArrayList<>();
        categorias.stream()
                .sorted(Comparator.comparingInt(CartaCategoria::getOrden)
                        .thenComparing(f -> f.getCategoria().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .forEach(f -> {
                    List<CartaMenuDTO.Producto> lista = porCategoria.get(f.getCategoria().getId());
                    if (lista != null && !lista.isEmpty()) {
                        grupos.add(new CartaMenuDTO.Grupo(
                                f.getCategoria().getId(), f.getCategoria().getNombre(), f.getCategoria().getTipo(), lista));
                    }
                });

        if (!otrosPlatos.isEmpty()) {
            grupos.add(new CartaMenuDTO.Grupo(null, "Otros platos", TipoCategoria.PLATO, otrosPlatos));
        }
        if (!otrasBebidas.isEmpty()) {
            grupos.add(new CartaMenuDTO.Grupo(null, "Otras bebidas", TipoCategoria.BEBIDA, otrasBebidas));
        }

        return new CartaMenuDTO(carta.getId(), carta.getNombre(), grupos);
    }

    @Transactional(readOnly = true)
    public ProductoCarta resolver(Long cartaId, TipoCategoria tipo, Long productoId) {
        if (tipo == TipoCategoria.PLATO) {
            CartaPlato fila = cartaPlatoRepository.findVisible(cartaId, productoId)
                    .orElseThrow(this::noDisponible);
            validarCategoria(cartaId, fila.getPlato().getCategoria());
            return new ProductoCarta(fila.getPlato().getId(), fila.getPlato().getNombre(),
                    precioDe(fila.getPrecio(), fila.getPlato().getPrecioVenta()), fila.isAgotado());
        }

        if (tipo == TipoCategoria.BEBIDA) {
            CartaBebida fila = cartaBebidaRepository.findVisible(cartaId, productoId)
                    .orElseThrow(this::noDisponible);
            validarCategoria(cartaId, fila.getBebida().getCategoria());
            return new ProductoCarta(fila.getBebida().getId(), fila.getBebida().getNombre(),
                    precioDe(fila.getPrecio(), fila.getBebida().getPrecioVenta()), fila.isAgotado());
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de producto no válido");
    }

    private void validarCategoria(Long cartaId, Categoria categoria) {
        if (categoria != null && !cartaCategoriaRepository.estaHabilitada(cartaId, categoria.getId())) {
            throw noDisponible();
        }
    }

    private BigDecimal precioDe(BigDecimal precioCarta, BigDecimal precioGeneral) {
        return precioCarta != null ? precioCarta : precioGeneral;
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El producto no está disponible en esta carta");
    }
}