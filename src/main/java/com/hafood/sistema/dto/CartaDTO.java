package com.hafood.sistema.dto;

public record CartaDTO(
        Long id,
        Long sedeId,
        String sedeNombre,
        String nombre,
        String codigo,
        boolean activo,
        boolean mostrarImagenes,
        String fondoMovilUrl,
        String fondoEscritorioUrl,
        String logoUrl
) {
}
