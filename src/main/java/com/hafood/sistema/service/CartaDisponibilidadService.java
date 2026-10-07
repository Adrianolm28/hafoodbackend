package com.hafood.sistema.service;

import com.hafood.sistema.domain.barra.Bebida;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.carta.CartaBebida;
import com.hafood.sistema.domain.carta.CartaCategoria;
import com.hafood.sistema.domain.carta.CartaPlato;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.domain.cocina.Plato;
import com.hafood.sistema.dto.CartaDisponibilidadDTO;
import com.hafood.sistema.dto.request.CartaDisponibilidadRequest;
import com.hafood.sistema.mapper.ImagenUrl;
import com.hafood.sistema.repository.BebidaRepository;
import com.hafood.sistema.repository.CartaBebidaRepository;
import com.hafood.sistema.repository.CartaCategoriaRepository;
import com.hafood.sistema.repository.CartaPlatoRepository;
import com.hafood.sistema.repository.CartaRepository;
import com.hafood.sistema.repository.CategoriaRepository;
import com.hafood.sistema.repository.PlatoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartaDisponibilidadService {

    private final CartaRepository cartaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PlatoRepository platoRepository;
    private final BebidaRepository bebidaRepository;
    private final CartaCategoriaRepository cartaCategoriaRepository;
    private final CartaPlatoRepository cartaPlatoRepository;
    private final CartaBebidaRepository cartaBebidaRepository;

    @Transactional(readOnly = true)
    public CartaDisponibilidadDTO obtener(Long cartaId) {
        return construir(buscarCarta(cartaId));
    }

    @Transactional
    public CartaDisponibilidadDTO actualizar(Long cartaId, CartaDisponibilidadRequest request) {
        Carta carta = buscarCarta(cartaId);
        actualizarCategorias(carta, vacia(request.categorias()));
        actualizarPlatos(carta, vacia(request.platos()));
        actualizarBebidas(carta, vacia(request.bebidas()));
        return construir(carta);
    }

    private void actualizarCategorias(Carta carta, List<CartaDisponibilidadRequest.CategoriaItem> lista) {
        Map<Long, CartaDisponibilidadRequest.CategoriaItem> items = lista.stream().collect(Collectors.toMap(
                CartaDisponibilidadRequest.CategoriaItem::categoriaId, Function.identity(), (a, b) -> b, LinkedHashMap::new));
        if (items.isEmpty()) {
            return;
        }
        Map<Long, CartaCategoria> existentes = cartaCategoriaRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getCategoria().getId(), Function.identity()));
        Map<Long, Categoria> categorias = categoriaRepository.findAllById(items.keySet()).stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));
        if (categorias.size() != items.size()) {
            throw noExiste("Alguna categoría no existe");
        }

        List<CartaCategoria> guardar = new ArrayList<>();
        for (CartaDisponibilidadRequest.CategoriaItem item : items.values()) {
            CartaCategoria fila = existentes.get(item.categoriaId());
            if (fila == null) {
                fila = CartaCategoria.builder().carta(carta).categoria(categorias.get(item.categoriaId())).build();
            }
            fila.setHabilitada(item.habilitada());
            fila.setOrden(item.orden());
            guardar.add(fila);
        }
        cartaCategoriaRepository.saveAll(guardar);
    }

    private void actualizarPlatos(Carta carta, List<CartaDisponibilidadRequest.ProductoItem> lista) {
        Map<Long, CartaDisponibilidadRequest.ProductoItem> items = lista.stream().collect(Collectors.toMap(
                CartaDisponibilidadRequest.ProductoItem::productoId, Function.identity(), (a, b) -> b, LinkedHashMap::new));
        if (items.isEmpty()) {
            return;
        }
        Map<Long, CartaPlato> existentes = cartaPlatoRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getPlato().getId(), Function.identity()));
        Map<Long, Plato> platos = platoRepository.findAllById(items.keySet()).stream()
                .collect(Collectors.toMap(Plato::getId, Function.identity()));
        if (platos.size() != items.size()) {
            throw noExiste("Algún plato no existe");
        }

        List<CartaPlato> guardar = new ArrayList<>();
        for (CartaDisponibilidadRequest.ProductoItem item : items.values()) {
            CartaPlato fila = existentes.get(item.productoId());
            if (fila == null) {
                fila = CartaPlato.builder().carta(carta).plato(platos.get(item.productoId())).build();
            }
            fila.setHabilitado(item.habilitado());
            fila.setAgotado(item.agotado());
            fila.setPrecio(item.precio());
            fila.setOrden(item.orden());
            guardar.add(fila);
        }
        cartaPlatoRepository.saveAll(guardar);
    }

    private void actualizarBebidas(Carta carta, List<CartaDisponibilidadRequest.ProductoItem> lista) {
        Map<Long, CartaDisponibilidadRequest.ProductoItem> items = lista.stream().collect(Collectors.toMap(
                CartaDisponibilidadRequest.ProductoItem::productoId, Function.identity(), (a, b) -> b, LinkedHashMap::new));
        if (items.isEmpty()) {
            return;
        }
        Map<Long, CartaBebida> existentes = cartaBebidaRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getBebida().getId(), Function.identity()));
        Map<Long, Bebida> bebidas = bebidaRepository.findAllById(items.keySet()).stream()
                .collect(Collectors.toMap(Bebida::getId, Function.identity()));
        if (bebidas.size() != items.size()) {
            throw noExiste("Alguna bebida no existe");
        }

        List<CartaBebida> guardar = new ArrayList<>();
        for (CartaDisponibilidadRequest.ProductoItem item : items.values()) {
            CartaBebida fila = existentes.get(item.productoId());
            if (fila == null) {
                fila = CartaBebida.builder().carta(carta).bebida(bebidas.get(item.productoId())).build();
            }
            fila.setHabilitado(item.habilitado());
            fila.setAgotado(item.agotado());
            fila.setPrecio(item.precio());
            fila.setOrden(item.orden());
            guardar.add(fila);
        }
        cartaBebidaRepository.saveAll(guardar);
    }

    private CartaDisponibilidadDTO construir(Carta carta) {
        Map<Long, CartaCategoria> filasCategoria = cartaCategoriaRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getCategoria().getId(), Function.identity()));
        Map<Long, CartaPlato> filasPlato = cartaPlatoRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getPlato().getId(), Function.identity()));
        Map<Long, CartaBebida> filasBebida = cartaBebidaRepository.findByCartaId(carta.getId()).stream()
                .collect(Collectors.toMap(f -> f.getBebida().getId(), Function.identity()));

        List<CartaDisponibilidadDTO.CategoriaItem> categorias = categoriaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(c -> {
                    CartaCategoria fila = filasCategoria.get(c.getId());
                    return new CartaDisponibilidadDTO.CategoriaItem(c.getId(), c.getNombre(), c.getTipo(),
                            fila != null && fila.isHabilitada(), fila == null ? 0 : fila.getOrden());
                })
                .toList();

        List<CartaDisponibilidadDTO.ProductoItem> platos = platoRepository.findActivosConCategoria().stream()
                .map(p -> {
                    CartaPlato fila = filasPlato.get(p.getId());
                    return new CartaDisponibilidadDTO.ProductoItem(p.getId(), p.getNombre(),
                            p.getCategoria() == null ? null : p.getCategoria().getId(), p.getPrecioVenta(),
                            fila != null && fila.isHabilitado(), fila != null && fila.isAgotado(),
                            fila == null ? null : fila.getPrecio(), fila == null ? 0 : fila.getOrden(),
                            ImagenUrl.de(p.getImagen()));
                })
                .toList();

        List<CartaDisponibilidadDTO.ProductoItem> bebidas = bebidaRepository.findActivosConCategoria().stream()
                .map(b -> {
                    CartaBebida fila = filasBebida.get(b.getId());
                    return new CartaDisponibilidadDTO.ProductoItem(b.getId(), b.getNombre(),
                            b.getCategoria() == null ? null : b.getCategoria().getId(), b.getPrecioVenta(),
                            fila != null && fila.isHabilitado(), fila != null && fila.isAgotado(),
                            fila == null ? null : fila.getPrecio(), fila == null ? 0 : fila.getOrden(),
                            ImagenUrl.de(b.getImagen()));
                })
                .toList();

        return new CartaDisponibilidadDTO(carta.getId(), categorias, platos, bebidas);
    }

    private Carta buscarCarta(Long id) {
        return cartaRepository.findById(id).orElseThrow(() -> noExiste("La carta no existe"));
    }

    private ResponseStatusException noExiste(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }

    private static <T> List<T> vacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
