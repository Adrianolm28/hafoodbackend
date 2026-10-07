package com.hafood.sistema.controller.cocina;

import com.hafood.sistema.dto.RecetaPlatoDTO;
import com.hafood.sistema.service.impl.IRecetaPlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recetas-platos")
@RequiredArgsConstructor
public class RecetaPlatoController {

    private final IRecetaPlatoService recetaPlatoService;

    @GetMapping
    public ResponseEntity<Page<RecetaPlatoDTO>> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(recetaPlatoService.listarTodos(pageable));
    }

    @GetMapping("/plato/{platoId}")
    public ResponseEntity<Page<RecetaPlatoDTO>> listarPorPlato(@PathVariable Long platoId, Pageable pageable) {
        return ResponseEntity.ok(recetaPlatoService.listarPorPlato(platoId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecetaPlatoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recetaPlatoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<RecetaPlatoDTO> agregarInsumo(@RequestBody RecetaPlatoDTO dto) {
        return new ResponseEntity<>(recetaPlatoService.agregarInsumo(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecetaPlatoDTO> actualizar(@PathVariable Long id, @RequestBody RecetaPlatoDTO dto) {
        return ResponseEntity.ok(recetaPlatoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id) {
        recetaPlatoService.eliminarInsumo(id);
        return ResponseEntity.noContent().build();
    }
}