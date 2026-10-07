package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.hafood.sistema.constant.UnidadIngreso;

import java.math.BigDecimal;

@Getter
@Setter
public class MovimientoRequest {

    @NotNull(message = "El ID del insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La ubicación es obligatoria")
    private Long ubicacionId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    private BigDecimal cantidad;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipoMovimiento;

    private UnidadIngreso unidadIngreso;
}