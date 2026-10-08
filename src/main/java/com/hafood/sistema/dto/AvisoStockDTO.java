package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoAlertaStock;

public record AvisoStockDTO(TipoAlertaStock tipo, Long lineaId, String mensaje) {
}