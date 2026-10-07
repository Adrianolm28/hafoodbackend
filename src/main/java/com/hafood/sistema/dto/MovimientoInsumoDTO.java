package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoMovimiento;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInsumoDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String unidadMedida;
    private Long sedeId;
    private String sedeNombre;
    private Long ubicacionId;
    private String ubicacionNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private TipoMovimiento tipoMovimiento;
    private BigDecimal cantidad;
    private BigDecimal stockResultante;
    private String motivo;
    private String referencia;
    private String presentacionNombre;
    private BigDecimal presentacionCantidad;
    private LocalDateTime fechaMovimiento;
}