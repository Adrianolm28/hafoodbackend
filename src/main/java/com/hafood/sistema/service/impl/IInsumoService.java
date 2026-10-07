package com.hafood.sistema.service.impl;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.dto.InsumoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IInsumoService {
    Page<InsumoDTO> listarTodos(AreaInsumo area, Pageable pageable);
    Page<InsumoDTO> listarActivos(AreaInsumo area, Pageable pageable);
    InsumoDTO obtenerPorId(Long id);
    InsumoDTO crear(InsumoDTO dto);
    InsumoDTO actualizar(Long id, InsumoDTO dto);
    void desactivar(Long id);
}