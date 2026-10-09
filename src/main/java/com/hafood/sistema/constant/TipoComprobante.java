package com.hafood.sistema.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoComprobante {
    FACTURA("01"),
    BOLETA("03");

    private final String codigo;
}