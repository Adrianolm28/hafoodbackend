package com.hafood.sistema.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AmbienteSunat {
    BETA("https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService"),
    PRODUCCION("https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService");

    private final String endpoint;
}