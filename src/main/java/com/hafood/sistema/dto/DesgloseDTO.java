package com.hafood.sistema.dto;

import com.hafood.sistema.constant.RegimenTributario;

import java.math.BigDecimal;
import java.util.List;

public record DesgloseDTO(
        boolean disponible,
        String motivo,
        RegimenTributario regimen,
        BigDecimal igvPorcentaje,
        BigDecimal ipmPorcentaje,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal opGravada,
        BigDecimal igv,
        BigDecimal ipm,
        BigDecimal total,
        List<Linea> lineas) {

    public record Linea(
            Long lineaId,
            String descripcion,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal total,
            BigDecimal baseImponible,
            BigDecimal igv,
            BigDecimal ipm) {
    }
}