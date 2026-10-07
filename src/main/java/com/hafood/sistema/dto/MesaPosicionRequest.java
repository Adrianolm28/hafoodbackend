package com.hafood.sistema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MesaPosicionRequest(
        @NotNull(message = "La posición es obligatoria") @Min(value = 0, message = "La posición no puede ser negativa") Integer posX,
        @NotNull(message = "La posición es obligatoria") @Min(value = 0, message = "La posición no puede ser negativa") Integer posY
) {
}