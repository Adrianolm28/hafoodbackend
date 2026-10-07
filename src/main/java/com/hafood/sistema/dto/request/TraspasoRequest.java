package com.hafood.sistema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TraspasoRequest {

    @NotNull(message = "La ubicación de origen es obligatoria")
    private Long origenId;

    @NotNull(message = "La ubicación de destino es obligatoria")
    private Long destinoId;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;

    private Boolean esPrestamo;

    @FutureOrPresent(message = "La fecha límite no puede ser pasada")
    private LocalDate fechaLimite;

    @NotEmpty(message = "El traspaso necesita al menos un insumo")
    @Valid
    private List<TraspasoDetalleRequest> detalles;
}