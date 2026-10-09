package com.hafood.sistema.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoDocumentoCliente {
    SIN_DOCUMENTO("0"),
    DNI("1"),
    CARNET_EXTRANJERIA("4"),
    RUC("6"),
    PASAPORTE("7");

    private final String codigo;
}