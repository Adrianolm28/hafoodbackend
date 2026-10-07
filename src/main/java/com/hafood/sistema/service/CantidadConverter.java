package com.hafood.sistema.service;

import com.hafood.sistema.constant.UnidadIngreso;
import com.hafood.sistema.domain.inventario.Insumo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CantidadConverter {

    private static final int MAX_DECIMALES = 3;

    public BigDecimal aUnidadBase(Insumo insumo, BigDecimal cantidad, UnidadIngreso unidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalido("La cantidad debe ser mayor a cero");
        }
        if (cantidad.stripTrailingZeros().scale() > MAX_DECIMALES) {
            throw invalido("La cantidad admite hasta " + MAX_DECIMALES + " decimales");
        }
        if (unidad == null || unidad == UnidadIngreso.BASE) {
            return cantidad;
        }
        if (insumo.getPresentacionNombre() == null || insumo.getPresentacionCantidad() == null) {
            throw invalido("El insumo " + insumo.getNombre() + " no tiene presentación configurada");
        }

        BigDecimal base = cantidad
                .multiply(insumo.getPresentacionCantidad())
                .setScale(MAX_DECIMALES, RoundingMode.HALF_UP);

        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalido("La cantidad es demasiado pequeña");
        }
        return base;
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}