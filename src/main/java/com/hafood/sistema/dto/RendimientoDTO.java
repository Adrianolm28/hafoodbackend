package com.hafood.sistema.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RendimientoDTO {
    private Long id;
    private Long insumoOrigenId;
    private String insumoOrigenNombre;
    private String unidadOrigen;
    private Long insumoDestinoId;
    private String insumoDestinoNombre;
    private String unidadDestino;
    private BigDecimal rendimiento;
}