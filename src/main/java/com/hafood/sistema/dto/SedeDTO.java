package com.hafood.sistema.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeDTO {
    private Long id;
    private String nombre;
    private String direccion;
}