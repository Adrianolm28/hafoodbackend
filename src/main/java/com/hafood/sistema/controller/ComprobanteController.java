package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ComprobanteDTO;
import com.hafood.sistema.dto.request.ComprobanteRequests;
import com.hafood.sistema.service.ComprobanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pos/cuentas/{cuentaId}/comprobante")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping
    public ResponseEntity<ComprobanteDTO> obtener(@PathVariable Long cuentaId, @AuthenticationPrincipal Usuario actor) {
        return comprobanteService.obtenerDeCuenta(cuentaId, actor)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<ComprobanteDTO> emitir(
            @PathVariable Long cuentaId,
            @RequestBody @Valid ComprobanteRequests.Emitir request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(comprobanteService.emitir(cuentaId, request, actor));
    }
}