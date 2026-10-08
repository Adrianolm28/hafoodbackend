package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.CategoriaCaja;
import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class CajaRequests {

    private CajaRequests() {
    }

    public record Crear(
            @NotNull(message = "La sede es obligatoria") Long sedeId,
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 60, message = "El nombre no puede superar los 60 caracteres") String nombre,
            @NotEmpty(message = "Elige al menos una sección") List<Long> seccionIds
    ) {
    }

    public record Actualizar(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 60, message = "El nombre no puede superar los 60 caracteres") String nombre,
            @NotNull(message = "Indica si la caja está activa") Boolean activa,
            @NotEmpty(message = "Elige al menos una sección") List<Long> seccionIds
    ) {
    }

    public record Denominacion(
            @NotNull(message = "La denominación es obligatoria") BigDecimal valor,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 0, message = "La cantidad no puede ser negativa")
            @Max(value = 100000, message = "La cantidad es demasiado alta") Integer cantidad
    ) {
    }

    public record LineaConteo(
            @NotNull(message = "El método es obligatorio") MetodoPago metodo,
            MarcaTarjeta marca,
            @NotNull(message = "La moneda es obligatoria") Moneda moneda,
            @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal contado,
            @Valid List<Denominacion> denominaciones
    ) {
    }

    public record Abrir(@NotNull(message = "El fondo inicial es obligatorio") @Valid List<LineaConteo> fondos) {
    }

    public record Movimiento(
            @NotNull(message = "La moneda es obligatoria") Moneda moneda,
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal monto,
            @NotNull(message = "La categoría es obligatoria") CategoriaCaja categoria,
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres") String motivo,
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String beneficiario,
            @NotBlank(message = "La clave de la operación es obligatoria")
            @Size(max = 64, message = "La clave de la operación es demasiado larga") String claveIdempotencia
    ) {
    }

    public record AnularMovimiento(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres") String motivo,
            CuentaRequests.Autorizador autorizador
    ) {
    }

    public record Arqueo(
            @NotEmpty(message = "El conteo es obligatorio") @Valid List<LineaConteo> lineas,
            @Size(max = 300, message = "La observación no puede superar los 300 caracteres") String observacion
    ) {
    }

    public record Cierre(
            @NotEmpty(message = "El conteo es obligatorio") @Valid List<LineaConteo> lineas,
            @Size(max = 300, message = "La observación no puede superar los 300 caracteres") String observacion,
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String entregadoA,
            boolean confirmarDiferencia,
            @Size(max = 300, message = "El motivo no puede superar los 300 caracteres") String motivoDiferencia
    ) {
    }

    public record PaseTurno(
            @NotEmpty(message = "El conteo es obligatorio") @Valid List<LineaConteo> lineas,
            @Size(max = 300, message = "La observación no puede superar los 300 caracteres") String observacion,
            boolean confirmarDiferencia,
            @Size(max = 300, message = "El motivo no puede superar los 300 caracteres") String motivoDiferencia,
            CuentaRequests.Autorizador receptor
    ) {
    }

    public record Revisar(
            @NotBlank(message = "El comentario es obligatorio")
            @Size(max = 300, message = "El comentario no puede superar los 300 caracteres") String comentario
    ) {
    }
}