package com.hafood.sistema.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PrestamoDetalleDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String unidadMedida;
    private String presentacionNombre;
    private BigDecimal presentacionCantidad;
    private BigDecimal cantidadPrestada;
    private BigDecimal cantidadDevuelta;
    private BigDecimal pendiente;
}