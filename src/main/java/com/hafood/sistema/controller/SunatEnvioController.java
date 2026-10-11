package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.EnvioResultadoDTO;
import com.hafood.sistema.service.ComprobanteArchivoService;
import com.hafood.sistema.service.SunatEnvioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/comprobantes/{comprobanteId}")
@RequiredArgsConstructor
public class SunatEnvioController {

    private final SunatEnvioService envioService;
    private final ComprobanteArchivoService archivoService;

    @PostMapping("/enviar")
    public ResponseEntity<EnvioResultadoDTO> enviar(@PathVariable Long comprobanteId,
                                                    @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(envioService.enviar(comprobanteId, actor));
    }

    @GetMapping("/cdr")
    public ResponseEntity<byte[]> cdr(@PathVariable Long comprobanteId, @AuthenticationPrincipal Usuario actor) {
        ComprobanteArchivoService.Descarga descarga = archivoService.descargarCdr(comprobanteId, actor);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombre()).build().toString())
                .body(descarga.contenido());
    }
}