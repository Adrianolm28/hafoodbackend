package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.TributarioDTO;
import com.hafood.sistema.dto.request.SunatRequests;
import com.hafood.sistema.service.SunatTributarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sunat/config")
@RequiredArgsConstructor
public class SunatTributarioController {

    private final SunatTributarioService tributarioService;

    @GetMapping("/tributario")
    public ResponseEntity<TributarioDTO> obtener() {
        return ResponseEntity.ok(tributarioService.obtener());
    }

    @PutMapping("/tributario")
    public ResponseEntity<TributarioDTO> guardar(
            @RequestBody @Valid SunatRequests.Tributario request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(tributarioService.guardar(request, actor));
    }

    @PostMapping("/tasas")
    public ResponseEntity<TributarioDTO> crearTasa(
            @RequestBody @Valid SunatRequests.Tasa request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(tributarioService.crearTasa(request, actor));
    }
}