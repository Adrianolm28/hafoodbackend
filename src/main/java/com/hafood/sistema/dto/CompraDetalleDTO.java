package com.hafood.sistema.dto;

import com.hafood.sistema.constant.UnidadIngreso;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDetalleDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String unidadMedida;
    private BigDecimal cantidadIngresada;
    private UnidadIngreso unidadIngreso;
    private BigDecimal cantidadRecibida;
    private BigDecimal cantidadEsperada;
    private BigDecimal diferencia;
    private BigDecimal montoPagado;
    private BigDecimal costoUnitario;
}