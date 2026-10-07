package com.hafood.sistema.controller;

import com.hafood.sistema.dto.PlatoDTO;
import com.hafood.sistema.service.impl.IPlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final IPlatoService platoService;

    @GetMapping
    public ResponseEntity<Page<PlatoDTO>> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(platoService.listarTodos(pageable));
    }

    @GetMapping("/activos")
    public ResponseEntity<Page<PlatoDTO>> listarActivos(Pageable pageable) {
        return ResponseEntity.ok(platoService.listarActivos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlatoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(platoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PlatoDTO> crear(@RequestBody PlatoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatoDTO> actualizar(@PathVariable Long id, @RequestBody PlatoDTO dto) {
        return ResponseEntity.ok(platoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        platoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}