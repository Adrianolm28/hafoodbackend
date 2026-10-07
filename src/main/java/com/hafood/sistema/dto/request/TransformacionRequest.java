package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.UnidadIngreso;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransformacionRequest {

    @NotNull(message = "La ubicación es obligatoria")
    private Long ubicacionId;

    @NotNull(message = "El insumo de origen es obligatorio")
    private Long insumoOrigenId;

    @NotNull(message = "El insumo de destino es obligatorio")
    private Long insumoDestinoId;

    @NotNull(message = "La cantidad de origen es obligatoria")
    @Positive(message = "La cantidad de origen debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    private BigDecimal cantidadOrigen;

    private UnidadIngreso unidadIngreso;

    @Positive(message = "La cantidad obtenida debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad obtenida admite hasta 3 decimales")
    private BigDecimal cantidadDestino;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;
}