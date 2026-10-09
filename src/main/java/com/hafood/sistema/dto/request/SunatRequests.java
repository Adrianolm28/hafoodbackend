package com.hafood.sistema.dto.request;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SunatRequests {

    private SunatRequests() {
    }

    public record Config(
            @NotBlank(message = "El RUC es obligatorio")
            @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos") String ruc,
            @NotBlank(message = "La razón social es obligatoria")
            @Size(max = 200, message = "La razón social es demasiado larga") String razonSocial,
            @Size(max = 200, message = "El nombre comercial es demasiado largo") String nombreComercial,
            @Size(max = 30, message = "El usuario SOL es demasiado largo") String usuarioSol,
            @Size(max = 100, message = "La clave SOL es demasiado larga") String claveSol,
            @NotNull(message = "El ambiente es obligatorio") AmbienteSunat ambiente
    ) {
    }

    public record Establecimiento(
            @NotBlank(message = "El código de establecimiento es obligatorio")
            @Pattern(regexp = "\\d{4}", message = "El código debe tener 4 dígitos (0000 para la principal)") String codigoEstablecimiento,
            @NotBlank(message = "La dirección fiscal es obligatoria")
            @Size(max = 250, message = "La dirección es demasiado larga") String direccionFiscal,
            @NotBlank(message = "El ubigeo es obligatorio")
            @Pattern(regexp = "\\d{6}", message = "El ubigeo debe tener 6 dígitos") String ubigeo,
            @NotBlank(message = "La serie de factura es obligatoria")
            @Pattern(regexp = "F[A-Z0-9]{3}", message = "La serie de factura empieza con F y tiene 4 caracteres (ej. F001)") String serieFactura,
            @NotBlank(message = "La serie de boleta es obligatoria")
            @Pattern(regexp = "B[A-Z0-9]{3}", message = "La serie de boleta empieza con B y tiene 4 caracteres (ej. B001)") String serieBoleta
    ) {
    }

    public record Tributario(
            @NotNull(message = "Elige el régimen tributario") RegimenTributario regimen,
            @DecimalMin(value = "0.01", message = "El umbral debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "El umbral tiene un formato no válido") BigDecimal umbralBoletaSinDocumento
    ) {
    }

    public record Tasa(
            @NotNull(message = "Elige el régimen") RegimenTributario regimen,
            @NotNull(message = "Elige el impuesto") TipoImpuesto tipo,
            @NotNull(message = "El porcentaje es obligatorio")
            @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
            @DecimalMax(value = "30.00", message = "El porcentaje no es válido")
            @Digits(integer = 2, fraction = 2, message = "El porcentaje admite hasta dos decimales") BigDecimal porcentaje,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDate vigenteDesde,
            LocalDate vigenteHasta
    ) {
    }
}