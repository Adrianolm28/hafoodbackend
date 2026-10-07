package com.hafood.sistema.controller;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.MovimientoInsumoDTO;
import com.hafood.sistema.dto.StockUbicacionDTO;
import com.hafood.sistema.dto.UbicacionDTO;
import com.hafood.sistema.dto.request.MovimientoRequest;
import com.hafood.sistema.service.InventarioService;
import com.hafood.sistema.service.UbicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final UbicacionService ubicacionService;

    @GetMapping("/ubicaciones")
    public ResponseEntity<List<UbicacionDTO>> listarUbicaciones(@RequestParam Long sedeId) {
        return ResponseEntity.ok(ubicacionService.listarPorSede(sedeId));
    }

    @GetMapping("/stock/sede/{sedeId}")
    public ResponseEntity<Page<StockUbicacionDTO>> stockPorSede(@PathVariable Long sedeId, Pageable pageable) {
        return ResponseEntity.ok(inventarioService.stockPorSede(sedeId, pageable));
    }

    @GetMapping("/stock/ubicacion/{ubicacionId}")
    public ResponseEntity<Page<StockUbicacionDTO>> stockPorUbicacion(@PathVariable Long ubicacionId, Pageable pageable) {
        return ResponseEntity.ok(inventarioService.stockPorUbicacion(ubicacionId, pageable));
    }

    @GetMapping("/movimientos/sede/{sedeId}")
    public ResponseEntity<Page<MovimientoInsumoDTO>> kardexPorSede(@PathVariable Long sedeId, Pageable pageable) {
        return ResponseEntity.ok(inventarioService.kardexPorSede(sedeId, pageable));
    }

    @GetMapping("/movimientos/ubicacion/{ubicacionId}")
    public ResponseEntity<Page<MovimientoInsumoDTO>> kardexPorUbicacion(@PathVariable Long ubicacionId, Pageable pageable) {
        return ResponseEntity.ok(inventarioService.kardexPorUbicacion(ubicacionId, pageable));
    }

    @PostMapping("/movimientos/entrada")
    public ResponseEntity<MovimientoInsumoDTO> registrarEntrada(
            @RequestBody @Valid MovimientoRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(inventarioService.registrarEntrada(request, actor.getId()));
    }

    @PostMapping("/movimientos/salida")
    public ResponseEntity<MovimientoInsumoDTO> registrarSalida(
            @RequestBody @Valid MovimientoRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(inventarioService.registrarSalida(request, actor.getId()));
    }
}