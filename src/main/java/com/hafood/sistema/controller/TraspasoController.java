package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.TraspasoDTO;
import com.hafood.sistema.dto.request.TraspasoRequest;
import com.hafood.sistema.service.TraspasoService;
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
@RequestMapping("/api/v1/inventario/traspasos")
@RequiredArgsConstructor
public class TraspasoController {

    private final TraspasoService traspasoService;

    @GetMapping
    public ResponseEntity<Page<TraspasoDTO>> listar(
            @RequestParam Long sedeId,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(traspasoService.listarPorSede(sedeId, pageable));
    }

    @PostMapping
    public ResponseEntity<TraspasoDTO> registrar(
            @RequestBody @Valid TraspasoRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(traspasoService.registrar(request, actor.getId()));
    }
}