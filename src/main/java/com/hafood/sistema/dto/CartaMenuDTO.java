package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoCategoria;

import java.math.BigDecimal;
import java.util.List;

public record CartaMenuDTO(Long cartaId, String nombre, List<Grupo> grupos) {

    public record Grupo(Long categoriaId, String nombre, TipoCategoria tipo, List<Producto> productos) {
    }

    public record Producto(
            Long productoId,
            TipoCategoria tipo,
            String nombre,
            String descripcion,
            BigDecimal precio,
            boolean agotado
    ) {
    }
}