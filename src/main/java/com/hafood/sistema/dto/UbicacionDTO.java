package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoUbicacion;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UbicacionDTO {
    private Long id;
    private Long sedeId;
    private String sedeNombre;
    private TipoUbicacion tipo;
    private Long seccionId;
    private String nombre;
    private Boolean activo;
}