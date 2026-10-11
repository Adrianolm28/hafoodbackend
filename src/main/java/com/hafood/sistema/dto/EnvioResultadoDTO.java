package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoSunat;

public record EnvioResultadoDTO(EstadoSunat estado, String codigo, String mensaje) {
}