package com.hafood.sistema.controller;

import com.hafood.sistema.dto.CartaPublicaDTO;
import com.hafood.sistema.service.CartaPublicaService;
import com.hafood.sistema.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/publico")
@RequiredArgsConstructor
public class PublicoController {

    private static final MediaType WEBP = MediaType.parseMediaType("image/webp");

    private final CartaPublicaService cartaPublicaService;
    private final ImageService imageService;

    @GetMapping("/cartas/{codigo}")
    public ResponseEntity<CartaPublicaDTO> obtenerCarta(@PathVariable String codigo) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(cartaPublicaService.obtener(codigo));
    }

    @GetMapping("/imagenes/{carpeta}/{archivo}")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable String carpeta, @PathVariable String archivo) {
        byte[] contenido = imageService.leer(carpeta, archivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La imagen no existe"));
        return ResponseEntity.ok()
                .contentType(WEBP)
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(contenido);
    }
}
