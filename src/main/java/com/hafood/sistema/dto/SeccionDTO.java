package com.hafood.sistema.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionDTO {
    private Long id;
    private String nombre;
    private Long sedeId;
    private String sedeNombre;
    private Long jefeMozoId;
    private String jefeMozoNombre;
    private Long jefeBartenderId;
    private String jefeBartenderNombre;
    private Boolean activo;
    private Integer orden;
}