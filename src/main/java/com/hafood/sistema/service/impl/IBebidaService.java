package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.BebidaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBebidaService {
    Page<BebidaDTO> listarTodos(Pageable pageable);
    Page<BebidaDTO> listarActivos(Pageable pageable);
    BebidaDTO obtenerPorId(Long id);
    BebidaDTO crear(BebidaDTO bebidaDTO);
    BebidaDTO actualizar(Long id, BebidaDTO bebidaDTO);
    void desactivar(Long id);
}