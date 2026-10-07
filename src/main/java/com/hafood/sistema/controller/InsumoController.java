package com.hafood.sistema.controller;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.dto.InsumoDTO;
import com.hafood.sistema.service.impl.IInsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final IInsumoService insumoService;

    @GetMapping
    public ResponseEntity<Page<InsumoDTO>> listarTodos(
            @RequestParam(required = false) AreaInsumo area,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(insumoService.listarTodos(area, pageable));
    }

    @GetMapping("/activos")
    public ResponseEntity<Page<InsumoDTO>> listarActivos(
            @RequestParam(required = false) AreaInsumo area,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(insumoService.listarActivos(area, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsumoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<InsumoDTO> crear(@RequestBody InsumoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsumoDTO> actualizar(@PathVariable Long id, @RequestBody InsumoDTO dto) {
        return ResponseEntity.ok(insumoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        insumoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}