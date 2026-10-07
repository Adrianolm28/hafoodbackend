package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.PrestamoDTO;
import com.hafood.sistema.dto.request.DevolucionRequest;
import com.hafood.sistema.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventario/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    public ResponseEntity<Page<PrestamoDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "true") boolean soloPendientes,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(prestamoService.listar(sedeId, rol, soloPendientes, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.obtener(id));
    }

    @PostMapping("/{id}/devoluciones")
    public ResponseEntity<PrestamoDTO> devolver(
            @PathVariable Long id,
            @RequestBody @Valid DevolucionRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(prestamoService.registrarDevolucion(id, request, actor.getId()));
    }
}