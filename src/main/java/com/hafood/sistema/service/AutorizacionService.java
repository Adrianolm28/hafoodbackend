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

    private static final Set<String> ROLES_SUPERVISOR =
            Set.of("ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_ADMINISTRADOR", "ROLE_ENCARGADO_SEDE");
    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 15;
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final SedeAccesoService sedeAccesoService;

    public boolean esSupervisor(Usuario usuario) {
        return usuario != null && usuario.getAuthorities().stream()
                .anyMatch(a -> ROLES_SUPERVISOR.contains(a.getAuthority()));
    }

    public Usuario resolver(Usuario actor, CuentaRequests.Autorizador credenciales, Long cuentaId, String ip) {
        if (esSupervisor(actor)) {
            return actor;
        }

        Long sedeId = cuentaRepository.findSedeIdById(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe"));

        return resolverPorSede(actor, credenciales, sedeId, ip);
    }

    public Usuario resolverPorSede(Usuario actor, CuentaRequests.Autorizador credenciales, Long sedeId, String ip) {
        if (esSupervisor(actor)) {
            return actor;
        }

        if (faltan(credenciales)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se necesita la autorización de un encargado o administrador");
        }

        Usuario autorizador = autenticar(credenciales, ip);

        if (!esSupervisor(autorizador)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa persona no puede autorizar esta operación");
        }

        try {
            sedeAccesoService.exigirAcceso(autorizador, sedeId);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa persona no pertenece a la sede");
        }

        return autorizador;
    }

    public Usuario autenticar(CuentaRequests.Autorizador credenciales, String ip) {
        if (faltan(credenciales)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el usuario y la contraseña");
        }

        String username = credenciales.username().trim();

        if (loginAttemptService.estaBloqueado(username, ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intenta nuevamente en 15 minutos.");
        }

        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null) {
            loginAttemptService.registrarFallo(username, ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CREDENCIALES_INVALIDAS);
        }

        if (usuario.getBloqueadoHasta() != null && Instant.now().isBefore(usuario.getBloqueadoHasta())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Esa cuenta está bloqueada temporalmente por múltiples intentos fallidos.");
        }

        if (!passwordEncoder.matches(credenciales.password(), usuario.getPassword())) {
            loginAttemptService.registrarFallo(username, ip);
            registrarFalloEnCuenta(usuario);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CREDENCIALES_INVALIDAS);
        }

        loginAttemptService.registrarExito(username, ip);

        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }

        return usuario;
    }

    private boolean faltan(CuentaRequests.Autorizador credenciales) {
        return credenciales == null
                || credenciales.username() == null || credenciales.username().isBlank()
                || credenciales.password() == null || credenciales.password().isEmpty();
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