package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.SeccionDTO;
import com.hafood.sistema.service.SeccionService;
import com.hafood.sistema.service.SedeAccesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionService seccionService;
    private final SedeAccesoService sedeAccesoService;

    @GetMapping
    public ResponseEntity<Page<SeccionDTO>> listar(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal Usuario actor) {
        Long sedeFija = sedeAccesoService.sedeFija(actor);

        return ResponseEntity.ok(sedeFija == null
                ? seccionService.listar(pageable)
                : seccionService.listarPorSede(sedeFija, pageable));
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<Page<SeccionDTO>> listarPorSede(
            @PathVariable Long sedeId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        return ResponseEntity.ok(seccionService.listarPorSede(sedeId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeccionDTO> obtenerPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        SeccionDTO dto = seccionService.obtenerPorId(id);
        sedeAccesoService.exigirAcceso(actor, dto.getSedeId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<SeccionDTO> crear(@RequestBody SeccionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seccionService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeccionDTO> actualizar(@PathVariable Long id, @RequestBody SeccionDTO dto) {
        return ResponseEntity.ok(seccionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        seccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}