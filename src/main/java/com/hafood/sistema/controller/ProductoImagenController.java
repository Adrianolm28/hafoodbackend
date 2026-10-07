package com.hafood.sistema.controller;

import com.hafood.sistema.dto.ImagenDTO;
import com.hafood.sistema.service.ProductoImagenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/catalogo/imagenes")
@RequiredArgsConstructor
public class ProductoImagenController {

    private final ProductoImagenService productoImagenService;

    @PostMapping(value = "/platos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenDTO> subirPlato(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(productoImagenService.subirPlato(id, archivo));
    }

    @DeleteMapping("/platos/{id}")
    public ResponseEntity<Void> quitarPlato(@PathVariable Long id) {
        productoImagenService.quitarPlato(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/bebidas/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenDTO> subirBebida(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(productoImagenService.subirBebida(id, archivo));
    }

    @DeleteMapping("/bebidas/{id}")
    public ResponseEntity<Void> quitarBebida(@PathVariable Long id) {
        productoImagenService.quitarBebida(id);
        return ResponseEntity.noContent().build();
    }
}
