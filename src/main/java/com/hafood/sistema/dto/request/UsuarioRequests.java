package com.hafood.sistema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioRequests {

    private UsuarioRequests() {
    }

    public record Password(@NotBlank @Size(max = 72) String password) {
    }

    public record Estado(@NotNull Boolean activo) {
    }
}