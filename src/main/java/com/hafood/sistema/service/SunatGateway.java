package com.hafood.sistema.service;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;

public interface SunatGateway {

    record Credenciales(String usuario, String clave) {

        @Override
        public String toString() {
            return "Credenciales[usuario=" + usuario + "]";
        }
    }

    record RespuestaSunat(EstadoSunat estado, String codigo, String mensaje, byte[] cdrZip) {
    }

    RespuestaSunat enviarComprobante(AmbienteSunat ambiente, Credenciales credenciales,
                                     String nombreBase, byte[] xmlFirmado);
}