package com.hafood.sistema.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ImpuestoCalculador {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public record Desglose(BigDecimal base, BigDecimal igv, BigDecimal ipm) {
    }

    private ImpuestoCalculador() {
    }

    public static Desglose desglosar(BigDecimal totalConImpuestos, BigDecimal igvPorcentaje, BigDecimal ipmPorcentaje) {
        BigDecimal total = totalConImpuestos.setScale(2, RoundingMode.HALF_UP);
        BigDecimal suma = igvPorcentaje.add(ipmPorcentaje);

        if (suma.signum() == 0) {
            return new Desglose(total, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
        }

        BigDecimal factor = BigDecimal.ONE.add(suma.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal base = total.divide(factor, 2, RoundingMode.HALF_UP);
        BigDecimal impuesto = total.subtract(base);
        BigDecimal igv = impuesto.multiply(igvPorcentaje).divide(suma, 2, RoundingMode.HALF_UP);

        return new Desglose(base, igv, impuesto.subtract(igv));
    }
}