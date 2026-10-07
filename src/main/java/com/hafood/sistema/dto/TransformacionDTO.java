package com.hafood.sistema.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TransformacionDTO {
    private Long id;
    private Long ubicacionId;
    private String ubicacionNombre;
    private Long sedeId;
    private String sedeNombre;
    private Long insumoOrigenId;
    private String insumoOrigenNombre;
    private String unidadOrigen;
    private BigDecimal cantidadOrigen;
    private Long insumoDestinoId;
    private String insumoDestinoNombre;
    private String unidadDestino;
    private BigDecimal cantidadDestino;
    private boolean estimada;
    private Long usuarioId;
    private String usuarioNombre;
    private String observacion;
    private LocalDateTime fecha;
}