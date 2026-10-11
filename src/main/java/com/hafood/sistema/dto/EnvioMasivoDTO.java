package com.hafood.sistema.dto;

public record EnvioMasivoDTO(int aceptados, int rechazados, int conError, int omitidos,
                             boolean quedanMas, String primerMotivo) {
}