package com.hafood.sistema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hafood.sistema.constant.Role;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Role role;
    private Long sedeId;
    private String sedeNombre;
    private Long seccionId;
    private String seccionNombre;
}