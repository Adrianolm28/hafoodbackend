package com.hafood.sistema.service.impl;

import com.hafood.sistema.dto.SeccionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISeccionService {
    Page<SeccionDTO> listar(Pageable pageable);
    Page<SeccionDTO> listarPorSede(Long sedeId, Pageable pageable);
    SeccionDTO obtenerPorId(Long id);
    SeccionDTO crear(SeccionDTO dto);
    SeccionDTO actualizar(Long id, SeccionDTO dto);
    void eliminar(Long id);
}
