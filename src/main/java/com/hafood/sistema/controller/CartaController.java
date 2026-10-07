package com.hafood.sistema.controller;

import com.hafood.sistema.constant.TipoImagen;
import com.hafood.sistema.dto.CartaDTO;
import com.hafood.sistema.dto.CartaDisponibilidadDTO;
import com.hafood.sistema.dto.request.CartaDisponibilidadRequest;
import com.hafood.sistema.dto.request.CartaRequest;
import com.hafood.sistema.dto.request.DuplicarCartaRequest;
import com.hafood.sistema.service.CartaDisponibilidadService;
import com.hafood.sistema.service.CartaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cartas")
@RequiredArgsConstructor
public class CartaController {

    private final CartaService cartaService;
    private final CartaDisponibilidadService disponibilidadService;

    @GetMapping
    public ResponseEntity<List<CartaDTO>> listar(@RequestParam Long sedeId) {
        return ResponseEntity.ok(cartaService.listarPorSede(sedeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cartaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<CartaDTO> crear(@Valid @RequestBody CartaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CartaRequest request) {
        return ResponseEntity.ok(cartaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cartaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicar")
    public ResponseEntity<CartaDTO> duplicar(@PathVariable Long id, @Valid @RequestBody DuplicarCartaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartaService.duplicar(id, request));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<CartaDisponibilidadDTO> obtenerDisponibilidad(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadService.obtener(id));
    }

    @PutMapping("/{id}/disponibilidad")
    public ResponseEntity<CartaDisponibilidadDTO> actualizarDisponibilidad(
            @PathVariable Long id, @Valid @RequestBody CartaDisponibilidadRequest request) {
        return ResponseEntity.ok(disponibilidadService.actualizar(id, request));
    }

    @PostMapping(value = "/{id}/imagenes/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CartaDTO> subirImagen(@PathVariable Long id, @PathVariable String tipo,
                                                @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(cartaService.subirImagen(id, resolverTipo(tipo), archivo));
    }

    @DeleteMapping("/{id}/imagenes/{tipo}")
    public ResponseEntity<CartaDTO> quitarImagen(@PathVariable Long id, @PathVariable String tipo) {
        return ResponseEntity.ok(cartaService.quitarImagen(id, resolverTipo(tipo)));
    }

    private TipoImagen resolverTipo(String tipo) {
        return switch (tipo) {
            case "fondo-movil" -> TipoImagen.FONDO_MOVIL;
            case "fondo-escritorio" -> TipoImagen.FONDO_ESCRITORIO;
            case "logo" -> TipoImagen.LOGO;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de imagen no válido");
        };
    }
}
