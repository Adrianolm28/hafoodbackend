package com.hafood.sistema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DevolucionRequest {

    @NotNull(message = "La ubicación desde la que se devuelve es obligatoria")
    private Long origenId;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;

    @NotEmpty(message = "La devolución necesita al menos un insumo")
    @Valid
    private List<TraspasoDetalleRequest> detalles;
}