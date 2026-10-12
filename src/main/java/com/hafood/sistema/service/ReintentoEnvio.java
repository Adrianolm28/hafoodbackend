package com.hafood.sistema.service;

import java.time.Duration;
import java.time.Instant;

public final class ReintentoEnvio {

    private static final long BASE_MINUTOS = 5;
    private static final long TOPE_MINUTOS = 120;

    private ReintentoEnvio() {
    }

    public static Duration espera(int intentos) {
        if (intentos <= 0) {
            return Duration.ZERO;
        }

        long minutos = BASE_MINUTOS << Math.min(intentos - 1, 10);
        return Duration.ofMinutes(Math.min(minutos, TOPE_MINUTOS));
    }

    public static boolean corresponde(Integer intentos, Instant ultimoEnvio, Instant ahora, int maxIntentos) {
        int hechos = intentos == null ? 0 : intentos;

        if (hechos >= maxIntentos) {
            return false;
        }
        if (hechos == 0 || ultimoEnvio == null) {
            return true;
        }

        return !ahora.isBefore(ultimoEnvio.plus(espera(hechos)));
    }
}