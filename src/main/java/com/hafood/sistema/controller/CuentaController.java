package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.service.AutorizacionService;
import com.hafood.sistema.service.CuentaService;
import com.hafood.sistema.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.hafood.sistema.service.CuentaDescuentoService;
@RestController
@RequestMapping("/api/v1/pos/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;
    private final CuentaDescuentoService cuentaDescuentoService;
    private final AutorizacionService autorizacionService;

    @GetMapping("/{id}")
    public ResponseEntity<CuentaDTO> obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.obtener(id, actor));
    }

    @PostMapping
    public ResponseEntity<CuentaDTO> abrir(
            @RequestBody @Valid CuentaRequests.Abrir request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.abrir(request, actor));
    }

    @PostMapping("/{id}/descuentos")
    public ResponseEntity<CuentaDTO> aplicarDescuento(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.AplicarDescuento request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest http) {
        Usuario autorizador = autorizacionService.resolver(actor, request.autorizador(), id, ClientIp.de(http));
        return ResponseEntity.ok(cuentaDescuentoService.aplicar(id, request, actor, autorizador));
    }

    @PostMapping("/{id}/descuentos/{descuentoId}/quitar")
    public ResponseEntity<CuentaDTO> quitarDescuento(
            @PathVariable Long id,
            @PathVariable Long descuentoId,
            @RequestBody @Valid CuentaRequests.QuitarDescuento request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest http) {
        Usuario autorizador = autorizacionService.resolver(actor, request.autorizador(), id, ClientIp.de(http));
        return ResponseEntity.ok(cuentaDescuentoService.quitar(id, descuentoId, request, actor, autorizador));
    }

    @PostMapping("/{id}/lineas")
    public ResponseEntity<CuentaDTO> agregarLinea(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.AgregarLinea request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.agregarLinea(id, request, actor));
    }

    @PutMapping("/{id}/lineas/{lineaId}")
    public ResponseEntity<CuentaDTO> actualizarLinea(
            @PathVariable Long id,
            @PathVariable Long lineaId,
            @RequestBody @Valid CuentaRequests.ActualizarLinea request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.actualizarLinea(id, lineaId, request, actor));
    }

    @DeleteMapping("/{id}/lineas/{lineaId}")
    public ResponseEntity<Void> eliminarLinea(
            @PathVariable Long id,
            @PathVariable Long lineaId,
            @AuthenticationPrincipal Usuario actor) {
        cuentaService.eliminarLinea(id, lineaId, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<CuentaDTO> anular(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.Anular request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.anular(id, request, actor));
    }

    @PostMapping("/{id}/cambiar-mesa")
    public ResponseEntity<CuentaDTO> cambiarMesa(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.CambiarMesa request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.cambiarMesa(id, request, actor));
    }

    @PostMapping("/{id}/juntar-mesa")
    public ResponseEntity<CuentaDTO> juntarMesa(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.JuntarMesa request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.juntarMesa(id, request, actor));
    }

    @PutMapping("/{id}/mozo")
    public ResponseEntity<CuentaDTO> cambiarMozo(
            @PathVariable Long id,
            @RequestBody @Valid CuentaRequests.CambiarMozo request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(cuentaService.cambiarMozo(id, request, actor));
    }
}