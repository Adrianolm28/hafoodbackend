package com.hafood.sistema.config;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 15;

    private record Intento(int conteo, Instant bloqueadoHasta) {}

    private final ConcurrentHashMap<String, Intento> intentos = new ConcurrentHashMap<>();

    private String clave(String username, String ip) {
        return (username == null ? "?" : username.toLowerCase()) + "|" + ip;
    }

    public void registrarFallo(String username, String ip) {
        intentos.compute(clave(username, ip), (k, actual) -> {
            int nuevoConteo = (actual == null ? 0 : actual.conteo()) + 1;
            Instant bloqueo = nuevoConteo >= MAX_INTENTOS
                    ? Instant.now().plusSeconds(BLOQUEO_MINUTOS * 60)
                    : null;
            return new Intento(nuevoConteo, bloqueo);
        });
    }

    public void registrarExito(String username, String ip) {
        intentos.remove(clave(username, ip));
    }

    public boolean estaBloqueado(String username, String ip) {
        Intento actual = intentos.get(clave(username, ip));
        if (actual == null || actual.bloqueadoHasta() == null) return false;
        if (Instant.now().isAfter(actual.bloqueadoHasta())) {
            intentos.remove(clave(username, ip));
            return false;
        }
        return true;
    }
}