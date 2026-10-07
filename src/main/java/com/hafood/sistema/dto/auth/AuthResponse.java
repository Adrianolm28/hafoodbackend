package com.hafood.sistema.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String role;
    private Long id;
    private Long sedeId;
    private String sedeNombre;
    private Long seccionId;
    private String seccionNombre;
}