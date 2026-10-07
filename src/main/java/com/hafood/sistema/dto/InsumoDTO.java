package com.hafood.sistema.dto;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.constant.UnidadBase;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsumoDTO {
    private Long id;
    private String nombre;
    private String unidadMedida;
    private UnidadBase unidadBase;
    private String presentacionNombre;
    private BigDecimal presentacionCantidad;
    private BigDecimal costoUnitario;
    private Boolean controlEstricto;
    private AreaInsumo area;
    private Boolean activo;
}