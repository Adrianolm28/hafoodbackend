package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.UsuarioDTO;
import com.hafood.sistema.service.UsuarioService;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor
    ) {
        usuarioService.eliminarUsuario(id, actor);
        return ResponseEntity.noContent().build();
    }
}