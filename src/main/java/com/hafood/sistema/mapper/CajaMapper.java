package com.hafood.sistema.mapper;

import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.dto.CajaDTO;

public final class CajaMapper {

    private CajaMapper() {
    }

    public static CajaDTO.SesionResumen toResumen(CajaSesion s, boolean verDiferencias) {
        return new CajaDTO.SesionResumen(
                s.getId(),
                s.getCaja().getId(),
                s.getCaja().getNombre(),
                s.getEstado(),
                s.getAbiertaPor().getUsername(),
                s.getAbiertaEn(),
                s.getCerradaEn(),
                s.getCerradaPor() == null ? null : s.getCerradaPor().getUsername(),
                s.getTipoCierre(),
                s.getEntregadoA(),
                s.getTurnoAnteriorId(),
                s.isDiferenciaPendiente(),
                verDiferencias ? s.getDiferenciaPen() : null,
                verDiferencias ? s.getLineasConDiferencia() : null,
                s.getRevisadaPor() == null ? null : s.getRevisadaPor().getUsername(),
                s.getRevisadaEn(),
                s.getComentarioRevision()
        );
    }
}