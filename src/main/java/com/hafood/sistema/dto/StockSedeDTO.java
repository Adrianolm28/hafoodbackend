package com.hafood.sistema.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSedeDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String unidadMedida;
    private Long sedeId;
    private String sedeNombre;
    private BigDecimal cantidadActual;
}