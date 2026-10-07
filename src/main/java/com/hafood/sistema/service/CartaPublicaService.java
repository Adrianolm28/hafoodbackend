package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.carta.CartaCategoria;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.dto.CartaPublicaDTO;
import com.hafood.sistema.mapper.ImagenUrl;
import com.hafood.sistema.repository.CartaBebidaRepository;
import com.hafood.sistema.repository.CartaCategoriaRepository;
import com.hafood.sistema.repository.CartaPlatoRepository;
import com.hafood.sistema.repository.CartaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CartaPublicaService {

    private static final Pattern CODIGO = Pattern.compile("^[a-z0-9]{8}$");

    private final CartaRepository cartaRepository;
    private final CartaCategoriaRepository cartaCategoriaRepository;
    private final CartaPlatoRepository cartaPlatoRepository;
    private final CartaBebidaRepository cartaBebidaRepository;

    private record Item(Categoria categoria, TipoCategoria tipo, int orden, CartaPublicaDTO.Producto producto) {
    }

    @Transactional(readOnly = true)
    public CartaPublicaDTO obtener(String codigo) {
        String normalizado = codigo == null ? "" : codigo.toLowerCase(Locale.ROOT);
        if (!CODIGO.matcher(normalizado).matches()) {
            throw noDisponible();
        }
        Carta carta = cartaRepository.buscarPorCodigo(normalizado)
                .filter(Carta::isActivo)
                .orElseThrow(this::noDisponible);

        boolean fotos = carta.isMostrarImagenes();
        List<CartaCategoria> categorias = cartaCategoriaRepository.findVisibles(carta.getId());
        Set<Long> habilitadas = new HashSet<>();
        categorias.forEach(f -> habilitadas.add(f.getCategoria().getId()));

        List<Item> items = new ArrayList<>();
        cartaPlatoRepository.findVisibles(carta.getId()).forEach(f -> items.add(new Item(
                f.getPlato().getCategoria(), TipoCategoria.PLATO, f.getOrden(),
                new CartaPublicaDTO.Producto(f.getPlato().getNombre(), f.getPlato().getDescripcion(),
                        f.getPrecio() != null ? f.getPrecio() : f.getPlato().getPrecioVenta(), f.isAgotado(),
                        fotos ? ImagenUrl.de(f.getPlato().getImagen()) : null))));
        cartaBebidaRepository.findVisibles(carta.getId()).forEach(f -> items.add(new Item(
                f.getBebida().getCategoria(), TipoCategoria.BEBIDA, f.getOrden(),
                new CartaPublicaDTO.Producto(f.getBebida().getNombre(), f.getBebida().getDescripcion(),
                        f.getPrecio() != null ? f.getPrecio() : f.getBebida().getPrecioVenta(), f.isAgotado(),
                        fotos ? ImagenUrl.de(f.getBebida().getImagen()) : null))));

        items.sort(Comparator.comparingInt(Item::orden)
                .thenComparing(i -> i.producto().nombre(), String.CASE_INSENSITIVE_ORDER));

        Map<Long, List<CartaPublicaDTO.Producto>> porCategoria = new HashMap<>();
        List<CartaPublicaDTO.Producto> otrosPlatos = new ArrayList<>();
        List<CartaPublicaDTO.Producto> otrasBebidas = new ArrayList<>();
        for (Item item : items) {
            if (item.categoria() == null) {
                (item.tipo() == TipoCategoria.PLATO ? otrosPlatos : otrasBebidas).add(item.producto());
            } else if (habilitadas.contains(item.categoria().getId())) {
                porCategoria.computeIfAbsent(item.categoria().getId(), k -> new ArrayList<>()).add(item.producto());
            }
        }

        List<CartaPublicaDTO.Grupo> grupos = new ArrayList<>();
        categorias.stream()
                .sorted(Comparator.comparingInt(CartaCategoria::getOrden)
                        .thenComparing(f -> f.getCategoria().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .forEach(f -> {
                    List<CartaPublicaDTO.Producto> lista = porCategoria.get(f.getCategoria().getId());
                    if (lista != null && !lista.isEmpty()) {
                        grupos.add(new CartaPublicaDTO.Grupo(f.getCategoria().getNombre(), f.getCategoria().getTipo(), lista));
                    }
                });
        if (!otrosPlatos.isEmpty()) {
            grupos.add(new CartaPublicaDTO.Grupo("Otros platos", TipoCategoria.PLATO, otrosPlatos));
        }
        if (!otrasBebidas.isEmpty()) {
            grupos.add(new CartaPublicaDTO.Grupo("Otras bebidas", TipoCategoria.BEBIDA, otrasBebidas));
        }

        return new CartaPublicaDTO(
                carta.getSede().getNombre(),
                carta.getNombre(),
                fotos,
                ImagenUrl.de(carta.getFondoMovil()),
                ImagenUrl.de(carta.getFondoEscritorio()),
                ImagenUrl.de(carta.getLogo()),
                grupos);
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta carta no está disponible");
    }
}
