package com.hafood.sistema.controller;

import com.hafood.sistema.constant.EstacionComanda;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.EstacionLineaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.service.ComandaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class ComandaController {

    private final ComandaService comandaService;

    @PostMapping("/cuentas/{id}/comandas")
    public ResponseEntity<CuentaDTO> enviar(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.EnviarComanda request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(comandaService.enviar(id, request, actor));
    }

    @PutMapping("/lineas/{lineaId}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long lineaId,
            @RequestBody @Valid CuentaRequests.CambiarEstadoLinea request,
            @AuthenticationPrincipal Usuario actor) {
        comandaService.cambiarEstado(lineaId, request.estado(), actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estaciones/{estacion}/lineas")
    public ResponseEntity<List<EstacionLineaDTO>> listar(
            @PathVariable EstacionComanda estacion,
            @RequestParam Long sedeId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(comandaService.listar(estacion, sedeId, actor));
    }
}