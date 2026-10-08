package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoCajaSesion;
import com.hafood.sistema.constant.TipoArqueo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CajaDTO(
        Long id,
        Long sedeId,
        String nombre,
        boolean activa,
        List<SeccionRef> secciones,
        SesionResumen sesionAbierta
) {

    public record SeccionRef(Long id, String nombre) {
    }

    public record SesionResumen(
            Long id,
            Long cajaId,
            String cajaNombre,
            EstadoCajaSesion estado,
            String cajeraNombre,
            Instant abiertaEn,
            Instant cerradaEn,
            String cerradaPorNombre,
            TipoArqueo tipoCierre,
            String entregadoA,
            Long turnoAnteriorId,
            boolean diferenciaPendiente,
            BigDecimal diferenciaPen,
            Integer lineasConDiferencia,
            String revisadaPorNombre,
            Instant revisadaEn,
            String comentarioRevision
    ) {
    }
}