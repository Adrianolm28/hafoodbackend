package com.hafood.sistema.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaPlatoDTO {
    private Long id;
    private Long platoId;
    private String platoNombre;
    private Long insumoId;
    private String insumoNombre;
    private BigDecimal cantidad;
    private String unidadMedida;
}