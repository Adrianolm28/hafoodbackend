package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.DesgloseDTO;
import com.hafood.sistema.service.DesgloseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/cuentas/{cuentaId}/desglose")
@RequiredArgsConstructor
public class DesgloseController {

    private final DesgloseService desgloseService;

    @GetMapping
    public ResponseEntity<DesgloseDTO> obtener(@PathVariable Long cuentaId, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(desgloseService.obtener(cuentaId, actor));
    }
}