package com.hafood.sistema.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaBebidaDTO {
    private Long id;
    private Long bebidaId;
    private String bebidaNombre;
    private Long insumoId;
    private String insumoNombre;
    private BigDecimal cantidad;
    private String unidadMedida;
}