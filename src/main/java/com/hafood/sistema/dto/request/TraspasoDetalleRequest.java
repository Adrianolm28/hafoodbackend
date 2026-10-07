package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.UnidadIngreso;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TraspasoDetalleRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    private BigDecimal cantidad;

    private UnidadIngreso unidadIngreso;
}