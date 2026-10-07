package com.hafood.sistema.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDTO {
    private Long id;
    private Long sedeId;
    private String sedeNombre;
    private Long ubicacionId;
    private String ubicacionNombre;
    private LocalDate fechaCompra;
    private String compradoPor;
    private Long firmanteId;
    private String firmanteNombre;
    private Long registradoPorId;
    private String registradoPorNombre;
    private String observacion;
    private BigDecimal totalPagado;
    private LocalDateTime fechaRegistro;
    private Long proveedorId;
    private String proveedorNombre;
    private List<CompraDetalleDTO> detalles;
}