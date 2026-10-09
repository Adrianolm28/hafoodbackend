package com.hafood.sistema.controller;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ComprobanteResumenDTO;
import com.hafood.sistema.service.ComprobanteListadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/comprobantes")
@RequiredArgsConstructor
public class ComprobanteListadoController {

    private final ComprobanteListadoService listadoService;

    @GetMapping
    public ResponseEntity<List<ComprobanteResumenDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoSunat estado,
            @RequestParam(required = false) TipoComprobante tipo,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(listadoService.listar(sedeId, desde, hasta, estado, tipo, actor));
    }
}