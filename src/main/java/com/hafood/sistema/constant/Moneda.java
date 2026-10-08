package com.hafood.sistema.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Moneda {
    PEN(new BigDecimal("1.00")),
    USD(new BigDecimal("3.50"));

    private final BigDecimal tipoCambio;

    Moneda(BigDecimal tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public BigDecimal tipoCambio() {
        return tipoCambio;
    }

    public BigDecimal aSoles(BigDecimal monto) {
        return monto.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
    }
}