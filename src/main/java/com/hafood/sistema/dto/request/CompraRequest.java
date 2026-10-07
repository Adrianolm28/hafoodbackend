package com.hafood.sistema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CompraRequest {

    @NotNull(message = "El almacén es obligatorio")
    private Long ubicacionId;

    @NotNull(message = "La fecha de compra es obligatoria")
    @PastOrPresent(message = "La fecha de compra no puede ser futura")
    private LocalDate fechaCompra;

    @NotBlank(message = "Indica quién hizo la compra")
    @Size(max = 100, message = "El nombre del comprador no puede superar los 100 caracteres")
    private String compradoPor;

    @NotNull(message = "El firmante es obligatorio")
    private Long firmanteId;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;

    private Long proveedorId;

    @NotEmpty(message = "La compra necesita al menos un insumo")
    @Valid
    private List<CompraDetalleRequest> detalles;
}