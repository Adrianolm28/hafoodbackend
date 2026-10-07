package com.hafood.sistema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MesaTemporalRequest(
        @NotNull(message = "La sección es obligatoria") Long seccionId,
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        @Max(value = 50, message = "La capacidad no puede superar 50") Integer capacidad
) {
}