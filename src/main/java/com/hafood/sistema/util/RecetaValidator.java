package com.hafood.sistema.util;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.exception.ValidationException;

import java.math.BigDecimal;

public final class RecetaValidator {

    private static final int CANTIDAD_MAX_DECIMALES = 3;

    private RecetaValidator() {}

    public static void validarCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new ValidationException("La cantidad es obligatoria");
        }
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a cero");
        }
        if (cantidad.stripTrailingZeros().scale() > CANTIDAD_MAX_DECIMALES) {
            throw new ValidationException("La cantidad admite hasta 3 decimales");
        }
    }

    public static void validarUnidad(String unidadSolicitada, Insumo insumo) {
        if (unidadSolicitada == null || unidadSolicitada.isBlank()) {
            return;
        }
        if (!unidadSolicitada.trim().equalsIgnoreCase(insumo.getUnidadMedida())) {
            throw new ValidationException(
                    "La cantidad debe expresarse en " + insumo.getUnidadMedida() + ", la unidad base del insumo"
            );
        }
    }

    public static void validarArea(Insumo insumo, AreaInsumo requerida) {
        AreaInsumo area = insumo.getArea() == null ? AreaInsumo.AMBAS : insumo.getArea();

        if (area != AreaInsumo.AMBAS && area != requerida) {
            String destino = requerida == AreaInsumo.BARRA ? "barra" : "cocina";
            throw new ValidationException("El insumo " + insumo.getNombre() + " no se puede usar en " + destino);
        }
    }
}