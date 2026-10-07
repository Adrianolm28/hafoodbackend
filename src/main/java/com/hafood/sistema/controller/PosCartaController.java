package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CartaDTO;
import com.hafood.sistema.dto.CartaMenuDTO;
import com.hafood.sistema.service.CartaMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/cartas")
@RequiredArgsConstructor
public class PosCartaController {

    private final CartaMenuService cartaMenuService;

    @GetMapping
    public ResponseEntity<List<CartaDTO>> listar(@RequestParam Long sedeId, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cartaMenuService.listarActivas(sedeId, actor));
    }

    @GetMapping("/{cartaId}/menu")
    public ResponseEntity<CartaMenuDTO> menu(@PathVariable Long cartaId, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cartaMenuService.obtener(cartaId, actor));
    }
}