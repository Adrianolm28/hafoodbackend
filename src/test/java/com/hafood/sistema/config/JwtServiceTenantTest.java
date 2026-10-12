package com.hafood.sistema.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTenantTest {

    private final JwtService servicio = new JwtService();
    private final UserDetails usuario = User.withUsername("mozo1").password("x").roles("MOZO").build();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "secretKey",
                Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded()));
    }

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    @Test
    void aceptaElTokenEnSuTenant() {
        TenantContext.setCurrentTenant("solar");
        String token = servicio.generateToken(usuario);
        assertTrue(servicio.isTokenValid(token, usuario));
    }

    @Test
    void rechazaElTokenEnOtroTenant() {
        TenantContext.setCurrentTenant("solar");
        String token = servicio.generateToken(usuario);
        TenantContext.setCurrentTenant("restaurante");
        assertFalse(servicio.isTokenValid(token, usuario));
    }
}