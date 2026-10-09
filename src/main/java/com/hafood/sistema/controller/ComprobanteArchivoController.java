package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.service.ComprobanteArchivoService;
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
@RequestMapping("/api/v1/pos/comprobantes/{comprobanteId}/xml")
@RequiredArgsConstructor
public class ComprobanteArchivoController {

    private final ComprobanteArchivoService archivoService;

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable Long comprobanteId, @AuthenticationPrincipal Usuario actor) {
        ComprobanteArchivoService.Descarga descarga = archivoService.descargarXml(comprobanteId, actor);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombre()).build().toString())
                .body(descarga.contenido());
    }

    @PostMapping("/regenerar")
    public ResponseEntity<Void> regenerar(@PathVariable Long comprobanteId, @AuthenticationPrincipal Usuario actor) {
        archivoService.regenerarXml(comprobanteId, actor);
        return ResponseEntity.noContent().build();
    }
}