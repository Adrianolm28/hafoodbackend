package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ComprobanteRequests {

    private ComprobanteRequests() {
    }

    public record Cliente(
            @NotNull(message = "Elige el tipo de documento") TipoDocumentoCliente tipoDocumento,
            @Size(max = 20, message = "El documento es demasiado largo") String numeroDocumento,
            @Size(max = 200, message = "El nombre es demasiado largo") String nombre,
            @Size(max = 250, message = "La dirección es demasiado larga") String direccion,
            @Email(message = "El correo no es válido") @Size(max = 120, message = "El correo es demasiado largo") String correo,
            @Size(max = 20, message = "El teléfono es demasiado largo") String telefono) {
    }

    public record Emitir(
            @NotBlank(message = "Falta la clave de la operación") @Size(max = 64) String claveIdempotencia,
            @NotNull(message = "Elige factura o boleta") TipoComprobante tipo,
            @Valid Cliente cliente) {
    }
}