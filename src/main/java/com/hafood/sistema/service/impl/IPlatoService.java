package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.PlatoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPlatoService {
    Page<PlatoDTO> listarTodos(Pageable pageable);
    Page<PlatoDTO> listarActivos(Pageable pageable);
    PlatoDTO obtenerPorId(Long id);
    PlatoDTO crear(PlatoDTO platoDTO);
    PlatoDTO actualizar(Long id, PlatoDTO platoDTO);
    void desactivar(Long id);
}