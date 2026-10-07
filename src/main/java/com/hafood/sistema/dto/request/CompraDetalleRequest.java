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
public class CompraDetalleRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad recibida es obligatoria")
    @Positive(message = "La cantidad recibida debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    private BigDecimal cantidad;

    private UnidadIngreso unidadIngreso;

    @Positive(message = "La cantidad esperada debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad esperada admite hasta 3 decimales")
    private BigDecimal cantidadEsperada;

    @NotNull(message = "El monto pagado es obligatorio")
    @Positive(message = "El monto pagado debe ser mayor a cero")
    @Digits(integer = 12, fraction = 2, message = "El monto admite hasta 2 decimales")
    private BigDecimal montoPagado;
}