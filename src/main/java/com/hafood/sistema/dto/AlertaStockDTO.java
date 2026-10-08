package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoAlertaStock;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertaStockDTO(
        Long id,
        Long cuentaId,
        Long lineaId,
        Long insumoId,
        TipoAlertaStock tipo,
        BigDecimal cantidadFaltante,
        String descripcion,
        Instant creadaEn,
        boolean revisada,
        String revisadaPorNombre,
        Instant revisadaEn
) {
}