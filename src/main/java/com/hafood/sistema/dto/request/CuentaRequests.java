package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.constant.TipoDescuento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class CuentaRequests {

    private CuentaRequests() {
    }

    public record Abrir(
            @NotNull(message = "La mesa es obligatoria") Long mesaId,
            @NotNull(message = "La carta es obligatoria") Long cartaId,
            @NotNull(message = "El mozo es obligatorio") Long mozoId,
            @Min(value = 1, message = "Los comensales deben ser al menos 1")
            @Max(value = 50, message = "Los comensales no pueden superar 50") Integer comensales,
            @Size(max = 200, message = "La nota no puede superar los 200 caracteres") String nota
    ) {
    }

    public record AgregarLinea(
            @NotNull(message = "El tipo de producto es obligatorio") TipoCategoria tipo,
            @NotNull(message = "El producto es obligatorio") Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 99, message = "La cantidad no puede superar 99") Integer cantidad,
            @Size(max = 200, message = "La nota no puede superar los 200 caracteres") String nota,
            @NotBlank(message = "La clave de la operación es obligatoria")
            @Size(max = 64, message = "La clave de la operación es demasiado larga") String claveIdempotencia
    ) {
    }

    public record ActualizarLinea(
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 99, message = "La cantidad no puede superar 99") Integer cantidad,
            @Size(max = 200, message = "La nota no puede superar los 200 caracteres") String nota
    ) {
    }

    public record Anular(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres") String motivo
    ) {
    }

    public record CambiarMesa(
            @NotNull(message = "La mesa de origen es obligatoria") Long mesaOrigenId,
            @NotNull(message = "La mesa de destino es obligatoria") Long mesaDestinoId
    ) {
    }

    public record JuntarMesa(@NotNull(message = "La mesa es obligatoria") Long mesaId) {
    }

    public record CambiarMozo(@NotNull(message = "El mozo es obligatorio") Long mozoId) {
    }

    public record Autorizador(String username, String password) {

        @Override
        public String toString() {
            return "Autorizador[username=" + username + "]";
        }
    }

    public record AplicarDescuento(
            Long lineaId,
            @NotNull(message = "El tipo de descuento es obligatorio") TipoDescuento tipo,
            @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
            @Digits(integer = 8, fraction = 2, message = "El valor admite hasta 2 decimales") BigDecimal valor,
            @Min(value = 1, message = "Las unidades de cortesía deben ser al menos 1")
            @Max(value = 99, message = "Las unidades de cortesía no pueden superar 99") Integer cantidadCortesia,
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres") String motivo,
            Autorizador autorizador
    ) {
    }

    public record QuitarDescuento(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres") String motivo,
            Autorizador autorizador
    ) {
    }

    public record EnviarComanda(
            @NotBlank(message = "La clave de la operación es obligatoria")
            @Size(max = 64, message = "La clave de la operación es demasiado larga") String claveIdempotencia
    ) {
    }

    public record CambiarEstadoLinea(@NotNull(message = "El estado es obligatorio") EstadoLinea estado) {
    }
}