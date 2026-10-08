package com.hafood.sistema.controller;

import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaDTO;
import com.hafood.sistema.dto.CajaSesionDTO;
import com.hafood.sistema.dto.ConteoResultadoDTO;
import com.hafood.sistema.dto.LineaConteoDTO;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.service.AutorizacionService;
import com.hafood.sistema.service.CajaMovimientoService;
import com.hafood.sistema.service.CajaService;
import com.hafood.sistema.service.CajaSesionService;
import com.hafood.sistema.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;
    private final CajaSesionService cajaSesionService;
    private final CajaMovimientoService cajaMovimientoService;
    private final AutorizacionService autorizacionService;

    @GetMapping("/estado")
    public ResponseEntity<List<CajaDTO>> estado(@RequestParam Long sedeId, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaService.listar(sedeId, false, actor));
    }

    @GetMapping("/conteo-plantilla")
    public ResponseEntity<List<LineaConteoDTO>> plantilla() {
        return ResponseEntity.ok(cajaSesionService.plantilla());
    }

    @PostMapping("/{cajaId}/abrir")
    public ResponseEntity<CajaSesionDTO> abrir(
            @PathVariable Long cajaId,
            @RequestBody @Valid CajaRequests.Abrir request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaSesionService.abrir(cajaId, request, actor));
    }

    @GetMapping("/sesiones/{id}")
    public ResponseEntity<CajaSesionDTO> obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaSesionService.obtener(id, actor));
    }

    @GetMapping("/sesiones")
    public ResponseEntity<List<CajaDTO.SesionResumen>> historial(
            @RequestParam Long sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cajaId,
            @RequestParam(defaultValue = "false") boolean soloPendientes,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaSesionService.historial(sedeId, desde, hasta, cajaId, soloPendientes, actor));
    }

    @PostMapping("/sesiones/{id}/ingresos")
    public ResponseEntity<CajaSesionDTO> ingreso(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Movimiento request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaMovimientoService.registrar(id, TipoMovimientoCaja.INGRESO, request, actor));
    }

    @PostMapping("/sesiones/{id}/egresos")
    public ResponseEntity<CajaSesionDTO> egreso(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Movimiento request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaMovimientoService.registrar(id, TipoMovimientoCaja.EGRESO, request, actor));
    }

    @PostMapping("/sesiones/{id}/movimientos/{movimientoId}/anular")
    public ResponseEntity<CajaSesionDTO> anularMovimiento(
            @PathVariable Long id,
            @PathVariable Long movimientoId,
            @RequestBody @Valid CajaRequests.AnularMovimiento request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest http) {
        Long sedeId = cajaSesionService.sedeIdDe(id);
        Usuario autorizador = autorizacionService.resolverPorSede(actor, request.autorizador(), sedeId, ClientIp.de(http));
        return ResponseEntity.ok(cajaMovimientoService.anular(id, movimientoId, request, actor, autorizador));
    }

    @PostMapping("/sesiones/{id}/arqueos")
    public ResponseEntity<ConteoResultadoDTO> arqueo(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Arqueo request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaSesionService.arqueoParcial(id, request, actor));
    }

    @PostMapping("/sesiones/{id}/cierre")
    public ResponseEntity<ConteoResultadoDTO> cerrar(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Cierre request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaSesionService.cerrar(id, request, actor));
    }

    @PostMapping("/sesiones/{id}/pase-turno")
    public ResponseEntity<ConteoResultadoDTO> paseTurno(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.PaseTurno request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest http) {
        Usuario receptor = autorizacionService.autenticar(request.receptor(), ClientIp.de(http));
        return ResponseEntity.ok(cajaSesionService.pasarTurno(id, request, actor, receptor));
    }

    @PostMapping("/sesiones/{id}/revisar")
    public ResponseEntity<CajaSesionDTO> revisar(
            @PathVariable Long id,
            @RequestBody @Valid CajaRequests.Revisar request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cajaSesionService.revisar(id, request, actor));
    }
}