package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.RecetaBebidaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IRecetaBebidaService {

    Page<RecetaBebidaDTO> listarTodos(Pageable pageable);

    Page<RecetaBebidaDTO> listarPorBebida(Long bebidaId, Pageable pageable);

    RecetaBebidaDTO obtenerPorId(Long id);

    RecetaBebidaDTO agregarInsumo(RecetaBebidaDTO dto);

    RecetaBebidaDTO actualizar(Long id, RecetaBebidaDTO dto);

    void eliminarInsumo(Long id);
}