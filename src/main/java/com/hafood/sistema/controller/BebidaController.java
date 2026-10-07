package com.hafood.sistema.controller;

import com.hafood.sistema.dto.BebidaDTO;
import com.hafood.sistema.service.impl.IBebidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bebidas")
@RequiredArgsConstructor
public class BebidaController {

    private final IBebidaService bebidaService;

    @GetMapping
    public ResponseEntity<Page<BebidaDTO>> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(bebidaService.listarTodos(pageable));
    }

    @GetMapping("/activos")
    public ResponseEntity<Page<BebidaDTO>> listarActivos(Pageable pageable) {
        return ResponseEntity.ok(bebidaService.listarActivos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BebidaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bebidaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<BebidaDTO> crear(@RequestBody BebidaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bebidaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BebidaDTO> actualizar(@PathVariable Long id, @RequestBody BebidaDTO dto) {
        return ResponseEntity.ok(bebidaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        bebidaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}