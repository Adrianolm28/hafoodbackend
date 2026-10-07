package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.StockUbicacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockUbicacionRepository extends JpaRepository<StockUbicacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockUbicacion s where s.insumo.id = :insumoId and s.ubicacion.id = :ubicacionId")
    Optional<StockUbicacion> findForUpdate(@Param("insumoId") Long insumoId,
                                           @Param("ubicacionId") Long ubicacionId);

    Page<StockUbicacion> findByUbicacionId(Long ubicacionId, Pageable pageable);

    Page<StockUbicacion> findByUbicacionSedeId(Long sedeId, Pageable pageable);
}