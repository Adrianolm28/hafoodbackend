package com.hafood.sistema.dto;

import java.math.BigDecimal;

public record OpcionVueltoDTO(boolean usdDisponible, BigDecimal vueltoUsd, String motivo) {
}