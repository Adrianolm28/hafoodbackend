package com.hafood.sistema.controller.barra;

import com.hafood.sistema.dto.RecetaBebidaDTO;
import com.hafood.sistema.service.impl.IRecetaBebidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recetas-bebidas")
@RequiredArgsConstructor
public class RecetaBebidaController {

    private final IRecetaBebidaService recetaBebidaService;

    @GetMapping
    public ResponseEntity<Page<RecetaBebidaDTO>> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(recetaBebidaService.listarTodos(pageable));
    }

    @GetMapping("/bebida/{bebidaId}")
    public ResponseEntity<Page<RecetaBebidaDTO>> listarPorBebida(@PathVariable Long bebidaId, Pageable pageable) {
        return ResponseEntity.ok(recetaBebidaService.listarPorBebida(bebidaId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecetaBebidaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recetaBebidaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<RecetaBebidaDTO> agregarInsumo(@RequestBody RecetaBebidaDTO dto) {
        return new ResponseEntity<>(recetaBebidaService.agregarInsumo(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecetaBebidaDTO> actualizar(@PathVariable Long id, @RequestBody RecetaBebidaDTO dto) {
        return ResponseEntity.ok(recetaBebidaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id) {
        recetaBebidaService.eliminarInsumo(id);
        return ResponseEntity.noContent().build();
    }
}