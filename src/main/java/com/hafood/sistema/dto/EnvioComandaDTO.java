package com.hafood.sistema.dto;

import java.util.List;

public record EnvioComandaDTO(CuentaDTO cuenta, List<AvisoStockDTO> avisos) {
}