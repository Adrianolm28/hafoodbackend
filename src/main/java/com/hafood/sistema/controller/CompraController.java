package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CompraDTO;
import com.hafood.sistema.dto.request.CompraRequest;
import com.hafood.sistema.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventario/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @GetMapping
    public ResponseEntity<Page<CompraDTO>> listar(
            @RequestParam Long sedeId,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(compraService.listarPorSede(sedeId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<CompraDTO> registrar(
            @RequestBody @Valid CompraRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(request, actor.getId()));
    }
}