package com.hafood.sistema.dto;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.RegimenTributario;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionSunatDTO(
        Long id,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String usuarioSol,
        boolean tieneClaveSol,
        boolean tieneCertificado,
        String nombreCertificado,
        LocalDate certificadoVence,
        AmbienteSunat ambiente,
        boolean activa,
        RegimenTributario regimen,
        BigDecimal umbralBoletaSinDocumento
) {
}