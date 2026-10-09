package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class CobroRequests {

    private CobroRequests() {
    }

    public record Pagar(
            @NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,
            MarcaTarjeta marca,
            @NotNull(message = "La moneda es obligatoria") Moneda moneda,
            @NotNull(message = "El monto recibido es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal recibido,
            @DecimalMin(value = "0.01", message = "El monto a cobrar debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal aplicar,
            Moneda vueltoMoneda,
            @Size(max = 60, message = "La referencia no puede superar los 60 caracteres") String referencia,
            @NotBlank(message = "La clave de la operación es obligatoria")
            @Size(max = 60, message = "La clave de la operación es demasiado larga") String claveIdempotencia
    ) {
    }

    public record Propina(
            @NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,
            MarcaTarjeta marca,
            @NotNull(message = "La moneda es obligatoria") Moneda moneda,
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal monto,
            @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
            @DecimalMax(value = "100.00", message = "El porcentaje no puede superar 100")
            @Digits(integer = 3, fraction = 2, message = "El porcentaje admite hasta 2 decimales") BigDecimal porcentaje,
            @Size(max = 60, message = "La referencia no puede superar los 60 caracteres") String referencia,
            @NotBlank(message = "La clave de la operación es obligatoria")
            @Size(max = 60, message = "La clave de la operación es demasiado larga") String claveIdempotencia
    ) {
    }

    public record Item(
            @NotNull(message = "El producto es obligatorio") Long lineaId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 99, message = "La cantidad no puede superar 99") Integer cantidad
    ) {
    }

    public record Grupo(
            @NotEmpty(message = "Cada cuenta separada necesita al menos un producto")
            @Size(max = 100, message = "Demasiados productos en una cuenta") @Valid List<Item> items
    ) {
    }

    public record Separar(
            @NotEmpty(message = "Indica qué productos pasan a cada cuenta")
            @Size(max = 20, message = "No se pueden crear más de 20 cuentas separadas") @Valid List<Grupo> grupos
    ) {
    }
}