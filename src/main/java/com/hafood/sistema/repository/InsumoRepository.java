package com.hafood.sistema.repository;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.domain.inventario.Insumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Page<Insumo> findAllByActivo(Boolean activo, Pageable pageable);

    @Query("select i from Insumo i where i.area is null or i.area in :areas")
    Page<Insumo> findByAreas(@Param("areas") Collection<AreaInsumo> areas, Pageable pageable);

    @Query("select i from Insumo i where i.activo = true and (i.area is null or i.area in :areas)")
    Page<Insumo> findActivosByAreas(@Param("areas") Collection<AreaInsumo> areas, Pageable pageable);
}