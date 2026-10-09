package com.hafood.sistema.dto;

import com.hafood.sistema.constant.RegimenTributario;

import java.math.BigDecimal;
import java.util.List;

public record TributarioDTO(
        boolean configurada,
        RegimenTributario regimen,
        BigDecimal umbralBoletaSinDocumento,
        List<TasaImpuestoDTO> tasas) {
}