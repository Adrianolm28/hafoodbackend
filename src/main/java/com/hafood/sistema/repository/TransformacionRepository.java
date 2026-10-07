package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.Transformacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransformacionRepository extends JpaRepository<Transformacion, Long> {

    Page<Transformacion> findByUbicacionSedeId(Long sedeId, Pageable pageable);
}