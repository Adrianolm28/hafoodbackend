package com.hafood.sistema.dto;

public record ConteoResultadoDTO(
        boolean cerrada,
        boolean hayDiferencia,
        CajaSesionDTO sesion,
        Long nuevaSesionId
) {
}