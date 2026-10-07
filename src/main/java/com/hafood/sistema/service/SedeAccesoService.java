package com.hafood.sistema.service;

import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SedeAccesoService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public void exigirAcceso(Usuario actor, Long sedeId) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida");
        }

        Usuario usuario = usuarioRepository.findById(actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));

        if (tieneRol(usuario, "ROLE_SUPERADMIN") || tieneRol(usuario, "ROLE_ADMIN")) {
            return;
        }

        Sede sede = usuario.getSede();

        if (sede == null) {
            if (tieneRol(usuario, "ROLE_ADMINISTRADOR")) {
                return;
            }
            throw prohibido();
        }

        if (!sede.getId().equals(sedeId)) {
            throw prohibido();
        }
    }

    private boolean tieneRol(Usuario usuario, String rol) {
        return usuario.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }

    private ResponseStatusException prohibido() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sede");
    }
}