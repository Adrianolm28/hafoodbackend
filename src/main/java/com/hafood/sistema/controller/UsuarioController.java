package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.UsuarioDTO;
import com.hafood.sistema.dto.request.UsuarioRequests;
import com.hafood.sistema.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.mostrarUsuarios());
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(
            @RequestBody UsuarioDTO usuarioDTO,
            @AuthenticationPrincipal Usuario actor
    ) {
        return ResponseEntity.ok(usuarioService.agregarUsuario(usuarioDTO, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody UsuarioDTO usuarioDTO,
            @AuthenticationPrincipal Usuario actor
    ) {
        return ResponseEntity.ok(usuarioService.actualizamosUsuario(id, usuarioDTO, actor));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<UsuarioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioRequests.Estado request,
            @AuthenticationPrincipal Usuario actor
    ) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request.activo(), actor));
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<Void> restablecerPassword(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioRequests.Password request,
            @AuthenticationPrincipal Usuario actor
    ) {
        usuarioService.restablecerPassword(id, request.password(), actor);
        return ResponseEntity.noContent().build();
    }
}