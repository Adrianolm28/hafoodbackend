package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.AlertaStockDTO;
import com.hafood.sistema.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/alertas")
@RequiredArgsConstructor
public class AlertaStockController {

    private final AlertaStockService alertaStockService;

    @GetMapping
    public ResponseEntity<List<AlertaStockDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam(defaultValue = "true") boolean soloPendientes,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(alertaStockService.listar(sedeId, soloPendientes, actor));
    }

    @PostMapping("/{id}/revisar")
    public ResponseEntity<AlertaStockDTO> revisar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(alertaStockService.revisar(id, actor));
    }
}