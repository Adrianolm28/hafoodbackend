package com.hafood.sistema.dto;

public record SedeSunatDTO(
        Long sedeId,
        String sedeNombre,
        boolean configurada,
        String codigoEstablecimiento,
        String direccionFiscal,
        String ubigeo,
        String serieFactura,
        String serieBoleta
) {
}