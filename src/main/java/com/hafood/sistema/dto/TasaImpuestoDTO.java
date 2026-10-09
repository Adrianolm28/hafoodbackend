package com.hafood.sistema.dto;

import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TasaImpuestoDTO(
        Long id,
        RegimenTributario regimen,
        TipoImpuesto tipo,
        BigDecimal porcentaje,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean vigenteHoy) {
}