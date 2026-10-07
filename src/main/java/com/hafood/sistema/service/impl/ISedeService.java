package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.SedeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISedeService {
    Page<SedeDTO> listar(Pageable pageable);
    SedeDTO obtenerPorId(Long id);
    SedeDTO crear(SedeDTO dto);
    SedeDTO actualizar(Long id, SedeDTO dto);
    void eliminar(Long id);
}