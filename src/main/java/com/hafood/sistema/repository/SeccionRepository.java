package com.hafood.sistema.repository;

import com.hafood.sistema.domain.estructura.Seccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    boolean existsBySedeId(Long sedeId);
    Page<Seccion> findBySedeId(Long sedeId, Pageable pageable);
}