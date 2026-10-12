package com.hafood.sistema.service;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;

public interface SunatGateway {

    RespuestaSunat enviarComprobante(AmbienteSunat ambiente, Credenciales credenciales,
                                     String nombreBase, byte[] xmlFirmado);

    default RespuestaSunat consultarConstancia(AmbienteSunat ambiente, Credenciales credenciales,
                                               String rucEmisor, String tipoComprobante,
                                               String serie, long correlativo) {
        return new RespuestaSunat(EstadoSunat.ERROR_ENVIO, null,
                "La consulta de constancias no está disponible", null);
    }

    record Credenciales(String usuario, String clave) {
    }

    record RespuestaSunat(EstadoSunat estado, String codigo, String mensaje, byte[] cdrZip) {
    }
}