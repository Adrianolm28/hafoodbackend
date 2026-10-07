package com.hafood.sistema.controller;

import com.hafood.sistema.dto.SedeDTO;
import com.hafood.sistema.service.impl.ISedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final ISedeService sedeService;

    @GetMapping
    public ResponseEntity<Page<SedeDTO>> listar(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(sedeService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<SedeDTO> crear(@RequestBody SedeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SedeDTO> actualizar(@PathVariable Long id, @RequestBody SedeDTO dto) {
        return ResponseEntity.ok(sedeService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sedeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}