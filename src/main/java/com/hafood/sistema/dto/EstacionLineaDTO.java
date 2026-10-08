package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoLinea;

import java.time.Instant;

public record EstacionLineaDTO(
        Long lineaId,
        Long comandaId,
        Long cuentaId,
        String mesas,
        String mozo,
        String nombre,
        Integer cantidad,
        String nota,
        EstadoLinea estado,
        Instant enviadaEn,
        Instant anuladaEn,
        String motivoAnulacion
) { }