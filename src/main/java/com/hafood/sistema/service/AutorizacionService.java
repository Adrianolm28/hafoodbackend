package com.hafood.sistema.service;

import com.hafood.sistema.config.LoginAttemptService;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private static final Set<String> ROLES_AUTORIZADOS =
            Set.of("ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_ADMINISTRADOR", "ROLE_ENCARGADO_SEDE");
    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 15;
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña de quien autoriza incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final SedeAccesoService sedeAccesoService;

    public Usuario resolver(Usuario actor, CuentaRequests.Autorizador credenciales, Long cuentaId, String ip) {
        if (puedeAutorizar(actor)) {
            return actor;
        }

        if (credenciales == null
                || credenciales.username() == null || credenciales.username().isBlank()
                || credenciales.password() == null || credenciales.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se necesita la autorización de un encargado o administrador");
        }

        String username = credenciales.username().trim();

        if (loginAttemptService.estaBloqueado(username, ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intenta nuevamente en 15 minutos.");
        }

        Usuario autorizador = usuarioRepository.findByUsername(username).orElse(null);

        if (autorizador == null) {
            loginAttemptService.registrarFallo(username, ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CREDENCIALES_INVALIDAS);
        }

        if (autorizador.getBloqueadoHasta() != null && Instant.now().isBefore(autorizador.getBloqueadoHasta())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Esa cuenta está bloqueada temporalmente por múltiples intentos fallidos.");
        }

        if (!passwordEncoder.matches(credenciales.password(), autorizador.getPassword())) {
            loginAttemptService.registrarFallo(username, ip);
            registrarFalloEnCuenta(autorizador);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CREDENCIALES_INVALIDAS);
        }

        loginAttemptService.registrarExito(username, ip);

        if (autorizador.getIntentosFallidos() > 0 || autorizador.getBloqueadoHasta() != null) {
            autorizador.setIntentosFallidos(0);
            autorizador.setBloqueadoHasta(null);
            usuarioRepository.save(autorizador);
        }

        if (!puedeAutorizar(autorizador)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa persona no puede autorizar descuentos");
        }

        Long sedeId = cuentaRepository.findSedeIdById(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe"));

        try {
            sedeAccesoService.exigirAcceso(autorizador, sedeId);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa persona no pertenece a la sede de la cuenta");
        }

        return autorizador;
    }

    private boolean puedeAutorizar(Usuario usuario) {
        return usuario != null && usuario.getAuthorities().stream()
                .anyMatch(a -> ROLES_AUTORIZADOS.contains(a.getAuthority()));
    }

    private void registrarFalloEnCuenta(Usuario candidato) {
        int nuevosIntentos = candidato.getIntentosFallidos() + 1;
        candidato.setIntentosFallidos(nuevosIntentos);

        if (nuevosIntentos >= MAX_INTENTOS) {
            candidato.setBloqueadoHasta(Instant.now().plusSeconds(BLOQUEO_MINUTOS * 60));
        }

        usuarioRepository.save(candidato);
    }
}