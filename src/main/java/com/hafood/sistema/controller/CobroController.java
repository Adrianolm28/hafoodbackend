package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.OpcionVueltoDTO;
import com.hafood.sistema.dto.request.CobroRequests;
import com.hafood.sistema.service.CuentaCobroService;
import com.hafood.sistema.service.CuentaSeparacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/pos/cuentas")
@RequiredArgsConstructor
public class CobroController {

    private final CuentaCobroService cobroService;
    private final CuentaSeparacionService separacionService;

    @PostMapping("/{id}/precuenta")
    public ResponseEntity<CuentaDTO> precuenta(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.precuenta(id, actor));
    }

    @PostMapping("/{id}/reabrir")
    public ResponseEntity<CuentaDTO> reabrir(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.reabrir(id, actor));
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<CuentaDTO> pagar(
            @PathVariable Long id,
            @RequestBody @Valid CobroRequests.Pagar request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.pagar(id, request, actor));
    }

    @PostMapping("/{id}/propinas")
    public ResponseEntity<CuentaDTO> propina(
            @PathVariable Long id,
            @RequestBody @Valid CobroRequests.Propina request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.propina(id, request, actor));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<CuentaDTO> cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.cerrar(id, actor));
    }

    @PostMapping("/{id}/separar")
    public ResponseEntity<CuentaDTO> separar(
            @PathVariable Long id,
            @RequestBody @Valid CobroRequests.Separar request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(separacionService.separar(id, request, actor));
    }

    @PostMapping("/{id}/reunir")
    public ResponseEntity<CuentaDTO> reunir(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(separacionService.reunir(id, actor));
    }

    @GetMapping("/{id}/vuelto-opciones")
    public ResponseEntity<OpcionVueltoDTO> opcionesVuelto(
            @PathVariable Long id,
            @RequestParam BigDecimal vueltoPen,
            @RequestParam(required = false) BigDecimal recibidoUsd,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cobroService.opcionesVuelto(id, vueltoPen, recibidoUsd, actor));
    }
}