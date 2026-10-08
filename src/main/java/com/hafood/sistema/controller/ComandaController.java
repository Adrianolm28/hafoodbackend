package com.hafood.sistema.controller;

import com.hafood.sistema.constant.EstacionComanda;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.EnvioComandaDTO;
import com.hafood.sistema.dto.EstacionLineaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.service.AutorizacionService;
import com.hafood.sistema.service.ComandaService;
import com.hafood.sistema.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AutorizacionService autorizacionService;


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

    @PostMapping("/cuentas/{id}/comandas")
    public ResponseEntity<EnvioComandaDTO> enviar(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.EnviarComanda request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(comandaService.enviar(id, request, actor));
    }

    @PostMapping("/cuentas/{id}/lineas/{lineaId}/anular")
    public ResponseEntity<CuentaDTO> anularLinea(
            @PathVariable Long id,
            @PathVariable Long lineaId,
            @RequestBody @Valid CuentaRequests.AnularLinea request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest http) {
        Usuario autorizador = null;

        if (comandaService.requiereAutoridad(id, lineaId, actor)) {
            autorizador = autorizacionService.resolver(actor, request.autorizador(), id, ClientIp.de(http));
        }

        return ResponseEntity.ok(comandaService.anularLinea(id, lineaId, request, actor, autorizador));
    }

    @PostMapping("/lineas/{lineaId}/visto")
    public ResponseEntity<Void> marcarVisto(
            @PathVariable Long lineaId,
            @AuthenticationPrincipal Usuario actor) {
        comandaService.marcarAnulacionVista(lineaId, actor);
        return ResponseEntity.noContent().build();
    }
}