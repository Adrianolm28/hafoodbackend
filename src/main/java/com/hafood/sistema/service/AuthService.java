package com.hafood.sistema.service;

import com.hafood.sistema.config.JwtService;
import com.hafood.sistema.config.LoginAttemptService;
import com.hafood.sistema.config.TenantContext;
import com.hafood.sistema.constant.Role;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.auth.AuthRequest;
import com.hafood.sistema.dto.auth.AuthResponse;
import com.hafood.sistema.dto.auth.RegisterRequestDTO;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.repository.UsuarioRepository;
import com.hafood.sistema.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    private static final int MAX_INTENTOS_CUENTA = 5;
    private static final long BLOQUEO_CUENTA_MINUTOS = 15;

    public AuthResponse register(RegisterRequestDTO request) {
        PasswordPolicy.validar(request.getPassword());

        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ValidationException("No se pudo completar el registro con los datos proporcionados");
        }

        var user = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.MOZO)
                .build();

        usuarioRepository.save(user);

        String tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("tenantId", tenantId);

        var jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .id(user.getId())
                .build();
    }

    public AuthResponse login(AuthRequest request, String ip) {
        if (loginAttemptService.estaBloqueado(request.getUsername(), ip)) {
            throw new LockedException("Demasiados intentos fallidos. Intenta nuevamente en 15 minutos.");
        }

        // 2. Control de estado de cuenta persistido en la BD del Tenant
        var usuarioOpt = usuarioRepository.findByUsername(request.getUsername());
        if (usuarioOpt.isPresent()) {
            Usuario candidato = usuarioOpt.get();
            if (candidato.getBloqueadoHasta() != null) {
                if (Instant.now().isBefore(candidato.getBloqueadoHasta())) {
                    throw new LockedException("Esta cuenta está bloqueada temporalmente por múltiples intentos fallidos.");
                } else {
                    candidato.setIntentosFallidos(0);
                    candidato.setBloqueadoHasta(null);
                    usuarioRepository.save(candidato);
                }
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFallo(request.getUsername(), ip);
            registrarFalloEnCuenta(usuarioOpt.orElse(null));
            throw e;
        }

        loginAttemptService.registrarExito(request.getUsername(), ip);

        var user = usuarioOpt.orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (user.getIntentosFallidos() > 0 || user.getBloqueadoHasta() != null) {
            user.setIntentosFallidos(0);
            user.setBloqueadoHasta(null);
            usuarioRepository.save(user);
        }

        // 3. Firma de Claims con Aislamiento Tenant/Sede/Sección
        String currentTenant = TenantContext.getCurrentTenant();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("tenantId", currentTenant);
        extraClaims.put("sedeId", user.getSede() != null ? user.getSede().getId() : null);
        extraClaims.put("seccionId", user.getSeccion() != null ? user.getSeccion().getId() : null);

        var jwtToken = jwtService.generateToken(extraClaims, user);

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .id(user.getId())
                .sedeId(user.getSede() != null ? user.getSede().getId() : null)
                .sedeNombre(user.getSede() != null ? user.getSede().getNombre() : null)
                .seccionId(user.getSeccion() != null ? user.getSeccion().getId() : null)
                .seccionNombre(user.getSeccion() != null ? user.getSeccion().getNombre() : null)
                .build();
    }

    private void registrarFalloEnCuenta(Usuario candidato) {
        if (candidato == null) return;
        int nuevosIntentos = candidato.getIntentosFallidos() + 1;
        candidato.setIntentosFallidos(nuevosIntentos);
        if (nuevosIntentos >= MAX_INTENTOS_CUENTA) {
            candidato.setBloqueadoHasta(Instant.now().plusSeconds(BLOQUEO_CUENTA_MINUTOS * 60));
        }
        usuarioRepository.save(candidato);
    }
}