package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Page<Compra> findByUbicacionSedeId(Long sedeId, Pageable pageable);
}