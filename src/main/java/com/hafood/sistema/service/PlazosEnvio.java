package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoComprobante;

public final class PlazosEnvio {

    private static final int DIAS_FACTURA = 3;
    private static final int DIAS_BOLETA = 7;

    private PlazosEnvio() {
    }

    public static int dias(TipoComprobante tipo) {
        return tipo == TipoComprobante.FACTURA ? DIAS_FACTURA : DIAS_BOLETA;
    }
}