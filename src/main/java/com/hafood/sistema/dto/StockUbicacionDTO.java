package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoUbicacion;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StockUbicacionDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String unidadMedida;
    private Long ubicacionId;
    private String ubicacionNombre;
    private TipoUbicacion tipoUbicacion;
    private Long sedeId;
    private String sedeNombre;
    private BigDecimal cantidadActual;
    private String presentacionNombre;
    private BigDecimal presentacionCantidad;
}