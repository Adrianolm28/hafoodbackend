package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ConfiguracionSunatDTO;
import com.hafood.sistema.dto.SedeSunatDTO;
import com.hafood.sistema.dto.request.SunatRequests;
import com.hafood.sistema.service.SunatConfigService;
import com.hafood.sistema.service.SunatSedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/sunat")
@RequiredArgsConstructor
public class SunatConfigController {

    private final SunatConfigService configService;
    private final SunatSedeService sedeService;

    @GetMapping("/config")
    public ResponseEntity<ConfiguracionSunatDTO> obtener() {
        return configService.obtener().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/config")
    public ResponseEntity<ConfiguracionSunatDTO> guardar(
            @RequestBody @Valid SunatRequests.Config request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(configService.guardar(request, actor));
    }

    @PostMapping(value = "/config/certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConfiguracionSunatDTO> certificado(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam("password") String password,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(configService.cargarCertificado(archivo, password, actor));
    }

    @PostMapping("/config/activar")
    public ResponseEntity<ConfiguracionSunatDTO> activar(@AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(configService.cambiarEstado(true, actor));
    }

    @PostMapping("/config/desactivar")
    public ResponseEntity<ConfiguracionSunatDTO> desactivar(@AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(configService.cambiarEstado(false, actor));
    }

    @GetMapping("/sedes/{sedeId}")
    public ResponseEntity<SedeSunatDTO> sede(@PathVariable Long sedeId, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(sedeService.obtener(sedeId, actor));
    }

    @PutMapping("/sedes/{sedeId}")
    public ResponseEntity<SedeSunatDTO> guardarSede(
            @PathVariable Long sedeId,
            @RequestBody @Valid SunatRequests.Establecimiento request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(sedeService.guardar(sedeId, request, actor));
    }
}