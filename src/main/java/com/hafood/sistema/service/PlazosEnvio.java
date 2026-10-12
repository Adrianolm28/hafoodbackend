package com.hafood.sistema.service;

import com.hafood.sistema.config.SunatProperties;
import com.hafood.sistema.constant.TipoComprobante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlazosEnvio {

    private final SunatProperties propiedades;

    public int dias(TipoComprobante tipo) {
        int dias = tipo == TipoComprobante.FACTURA
                ? propiedades.getPlazoFacturaDias()
                : propiedades.getPlazoBoletaDias();
        return Math.max(1, dias);
    }
}