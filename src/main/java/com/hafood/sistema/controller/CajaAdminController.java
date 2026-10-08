package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaDTO;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cajas")
@RequiredArgsConstructor
public class CajaAdminController {

    private final CajaService cajaService;

    @GetMapping
    public ResponseEntity<List<CajaDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam(defaultValue = "true") boolean incluirInactivas,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaService.listar(sedeId, incluirInactivas, actor));
    }

    @PostMapping
    public ResponseEntity<CajaDTO> crear(
            @RequestBody @Valid CajaRequests.Crear request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.crear(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CajaDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Actualizar request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaService.actualizar(id, request, actor));
    }
}