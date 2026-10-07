package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.RecetaPlatoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IRecetaPlatoService {

    Page<RecetaPlatoDTO> listarTodos(Pageable pageable);

    Page<RecetaPlatoDTO> listarPorPlato(Long platoId, Pageable pageable);

    RecetaPlatoDTO obtenerPorId(Long id);

    RecetaPlatoDTO agregarInsumo(RecetaPlatoDTO dto);

    RecetaPlatoDTO actualizar(Long id, RecetaPlatoDTO dto);

    void eliminarInsumo(Long id);
}