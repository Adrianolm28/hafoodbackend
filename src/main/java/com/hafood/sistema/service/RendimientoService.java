package com.hafood.sistema.service;

import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.inventario.InsumoRendimiento;
import com.hafood.sistema.dto.RendimientoDTO;
import com.hafood.sistema.dto.request.RendimientoRequest;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.InsumoRendimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RendimientoService {

    private final InsumoRendimientoRepository rendimientoRepository;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public List<RendimientoDTO> listar() {
        return rendimientoRepository.findAllByOrderByIdAsc().stream().map(Mapper::toDTO).toList();
    }

    @Transactional
    public RendimientoDTO guardar(RendimientoRequest request) {
        if (request.getInsumoOrigenId().equals(request.getInsumoDestinoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El origen y el destino deben ser insumos distintos");
        }

        Insumo origen = inventarioService.buscarInsumo(request.getInsumoOrigenId());
        Insumo destino = inventarioService.buscarInsumo(request.getInsumoDestinoId());

        InsumoRendimiento rendimiento = rendimientoRepository
                .findByInsumoOrigenIdAndInsumoDestinoId(origen.getId(), destino.getId())
                .orElseGet(() -> InsumoRendimiento.builder()
                        .insumoOrigen(origen)
                        .insumoDestino(destino)
                        .build());

        rendimiento.setRendimiento(request.getRendimiento());
        return Mapper.toDTO(rendimientoRepository.save(rendimiento));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!rendimientoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El rendimiento no existe");
        }
        rendimientoRepository.deleteById(id);
    }
}