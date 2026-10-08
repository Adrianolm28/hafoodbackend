package com.hafood.sistema.dto;

import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;

public record LineaConteoDTO(MetodoPago metodo, MarcaTarjeta marca, Moneda moneda, String etiqueta) {
}