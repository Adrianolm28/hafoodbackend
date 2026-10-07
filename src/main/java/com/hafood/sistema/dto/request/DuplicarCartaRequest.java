package com.hafood.sistema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DuplicarCartaRequest(
        @NotNull(message = "La sede es obligatoria")
        Long sedeId,
        @NotBlank(message = "El nombre de la carta es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {
}
