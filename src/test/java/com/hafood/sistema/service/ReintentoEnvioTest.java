package com.hafood.sistema.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReintentoEnvioTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T20:00:00Z");

    @Test
    void elPrimerEnvioSiempreCorresponde() {
        assertTrue(ReintentoEnvio.corresponde(null, null, AHORA, 8));
        assertTrue(ReintentoEnvio.corresponde(0, null, AHORA, 8));
    }

    @Test
    void laEsperaCreceYTieneTope() {
        assertEquals(Duration.ofMinutes(5), ReintentoEnvio.espera(1));
        assertEquals(Duration.ofMinutes(10), ReintentoEnvio.espera(2));
        assertEquals(Duration.ofMinutes(80), ReintentoEnvio.espera(5));
        assertEquals(Duration.ofMinutes(120), ReintentoEnvio.espera(6));
        assertEquals(Duration.ofMinutes(120), ReintentoEnvio.espera(40));
    }

    @Test
    void noReintentaAntesDeLaEspera() {
        assertFalse(ReintentoEnvio.corresponde(1, AHORA.minusSeconds(60), AHORA, 8));
        assertTrue(ReintentoEnvio.corresponde(1, AHORA.minusSeconds(300), AHORA, 8));
    }

    @Test
    void dejaDeReintentarAlLlegarAlMaximo() {
        assertFalse(ReintentoEnvio.corresponde(8, AHORA.minusSeconds(100_000), AHORA, 8));
        assertTrue(ReintentoEnvio.corresponde(7, AHORA.minusSeconds(100_000), AHORA, 8));
    }
}