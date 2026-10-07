package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.RendimientoDTO;
import com.hafood.sistema.dto.TransformacionDTO;
import com.hafood.sistema.dto.request.RendimientoRequest;
import com.hafood.sistema.dto.request.TransformacionRequest;
import com.hafood.sistema.service.RendimientoService;
import com.hafood.sistema.service.TransformacionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class TransformacionController {

    private final TransformacionService transformacionService;
    private final RendimientoService rendimientoService;

    @GetMapping("/transformaciones")
    public ResponseEntity<Page<TransformacionDTO>> listar(
            @RequestParam Long sedeId,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transformacionService.listarPorSede(sedeId, pageable));
    }

    @PostMapping("/transformaciones")
    public ResponseEntity<TransformacionDTO> registrar(
            @RequestBody @Valid TransformacionRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transformacionService.registrar(request, actor.getId()));
    }

    @GetMapping("/rendimientos")
    public ResponseEntity<List<RendimientoDTO>> listarRendimientos() {
        return ResponseEntity.ok(rendimientoService.listar());
    }

    @PostMapping("/rendimientos")
    public ResponseEntity<RendimientoDTO> guardarRendimiento(@RequestBody @Valid RendimientoRequest request) {
        return ResponseEntity.ok(rendimientoService.guardar(request));
    }

    @DeleteMapping("/rendimientos/{id}")
    public ResponseEntity<Void> eliminarRendimiento(@PathVariable Long id) {
        rendimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}