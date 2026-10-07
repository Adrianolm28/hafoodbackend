package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.PersonalDTO;
import com.hafood.sistema.dto.SiguienteCodigoDTO;
import com.hafood.sistema.service.PersonalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalService personalService;

    @GetMapping
    public ResponseEntity<Page<PersonalDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam(defaultValue = "false") boolean soloActivos,
            @PageableDefault(size = 10, sort = "codigo") Pageable pageable,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(personalService.listar(sedeId, soloActivos, pageable, actor));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<PersonalDTO>> listarActivos(
            @RequestParam Long sedeId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(personalService.listarActivos(sedeId, actor));
    }

    @GetMapping("/siguiente-codigo")
    public ResponseEntity<SiguienteCodigoDTO> siguienteCodigo(
            @RequestParam Long sedeId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(personalService.siguienteCodigo(sedeId, actor));
    }

    @PostMapping
    public ResponseEntity<PersonalDTO> crear(
            @RequestBody @Valid PersonalDTO dto,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personalService.crear(dto, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonalDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid PersonalDTO dto,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(personalService.actualizar(id, dto, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {
        personalService.desactivar(id, actor);
        return ResponseEntity.noContent().build();
    }
}