package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoCategoria;

import java.math.BigDecimal;
import java.util.List;

public record CartaDisponibilidadDTO(
        Long cartaId,
        List<CategoriaItem> categorias,
        List<ProductoItem> platos,
        List<ProductoItem> bebidas
) {

    public record CategoriaItem(
            Long categoriaId,
            String nombre,
            TipoCategoria tipo,
            boolean habilitada,
            int orden
    ) {
    }

    public record ProductoItem(
            Long productoId,
            String nombre,
            Long categoriaId,
            BigDecimal precioGeneral,
            boolean habilitado,
            boolean agotado,
            BigDecimal precio,
            int orden,
            String imagenUrl
    ) {
    }
}
