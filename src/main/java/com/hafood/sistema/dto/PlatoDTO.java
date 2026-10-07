package com.hafood.sistema.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioVenta;
    private Long categoriaId;
    private String categoriaNombre;
    private String imagenUrl;
    private Boolean activo;
}
