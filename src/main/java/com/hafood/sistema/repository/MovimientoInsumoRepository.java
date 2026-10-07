package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.MovimientoInsumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoInsumoRepository extends JpaRepository<MovimientoInsumo, Long> {
    Page<MovimientoInsumo> findBySedeId(Long sedeId, Pageable pageable);
    Page<MovimientoInsumo> findByInsumoIdAndSedeId(Long insumoId, Long sedeId, Pageable pageable);
    Page<MovimientoInsumo> findByUbicacionId(Long ubicacionId, Pageable pageable);
}