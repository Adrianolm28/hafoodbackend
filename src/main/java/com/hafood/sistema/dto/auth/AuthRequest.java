package com.hafood.sistema.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthRequest {
    private String username;
    private String password;
}