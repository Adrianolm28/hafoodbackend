package com.hafood.sistema.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RendimientoRequest {

    @NotNull(message = "El insumo de origen es obligatorio")
    private Long insumoOrigenId;

    @NotNull(message = "El insumo de destino es obligatorio")
    private Long insumoDestinoId;

    @NotNull(message = "El rendimiento es obligatorio")
    @Positive(message = "El rendimiento debe ser mayor a cero")
    @Digits(integer = 8, fraction = 6, message = "El rendimiento admite hasta 6 decimales")
    private BigDecimal rendimiento;
}