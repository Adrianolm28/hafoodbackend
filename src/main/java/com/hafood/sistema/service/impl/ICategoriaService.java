package com.hafood.sistema.service.impl;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.dto.CategoriaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICategoriaService {
    Page<CategoriaDTO> listarTodas(TipoCategoria tipo, Pageable pageable);
    Page<CategoriaDTO> listarActivas(TipoCategoria tipo, Pageable pageable);
    CategoriaDTO obtenerPorId(Long id);
    CategoriaDTO crear(CategoriaDTO dto);
    CategoriaDTO actualizar(Long id, CategoriaDTO dto);
    void desactivar(Long id);
}