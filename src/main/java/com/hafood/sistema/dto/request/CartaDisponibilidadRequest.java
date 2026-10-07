package com.hafood.sistema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CartaDisponibilidadRequest(
        List<@Valid CategoriaItem> categorias,
        List<@Valid ProductoItem> platos,
        List<@Valid ProductoItem> bebidas
) {

    public record CategoriaItem(
            @NotNull(message = "La categoría es obligatoria")
            Long categoriaId,
            boolean habilitada,
            @Min(value = 0, message = "El orden no puede ser negativo")
            int orden
    ) {
    }

    public record ProductoItem(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,
            boolean habilitado,
            boolean agotado,
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
            @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales")
            BigDecimal precio,
            @Min(value = 0, message = "El orden no puede ser negativo")
            int orden
    ) {
    }
}
