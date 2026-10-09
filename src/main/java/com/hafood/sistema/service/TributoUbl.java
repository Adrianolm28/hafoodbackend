package com.hafood.sistema.service;

import java.math.BigDecimal;

final class TributoUbl {

    private TributoUbl() {
    }

    static BigDecimal monto(BigDecimal igv, BigDecimal ipm) {
        return igv.add(ipm);
    }

    static BigDecimal tasa(BigDecimal igvPorcentaje, BigDecimal ipmPorcentaje) {
        return igvPorcentaje.add(ipmPorcentaje);
    }
}