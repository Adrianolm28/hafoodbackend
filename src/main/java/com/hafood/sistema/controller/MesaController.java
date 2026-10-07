package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.MesaDTO;
import com.hafood.sistema.dto.MesaPosicionRequest;
import com.hafood.sistema.dto.MesaTemporalRequest;
import com.hafood.sistema.service.MesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;

    @GetMapping
    public ResponseEntity<List<MesaDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(mesaService.listar(sedeId, incluirInactivas, actor));
    }

    @PostMapping
    public ResponseEntity<MesaDTO> crear(
            @RequestBody @Valid MesaDTO dto,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mesaService.crear(dto, actor));
    }

    @PostMapping("/temporal")
    public ResponseEntity<MesaDTO> crearTemporal(
            @RequestBody @Valid MesaTemporalRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mesaService.crearTemporal(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MesaDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid MesaDTO dto,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(mesaService.actualizar(id, dto, actor));
    }

    @PutMapping("/{id}/posicion")
    public ResponseEntity<MesaDTO> moverPosicion(
            @PathVariable Long id,
            @RequestBody @Valid MesaPosicionRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(mesaService.moverPosicion(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {
        mesaService.desactivar(id, actor);
        return ResponseEntity.noContent().build();
    }
}