package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.Traspaso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraspasoRepository extends JpaRepository<Traspaso, Long> {

    @Query("select t from Traspaso t where t.origen.sede.id = :sedeId or t.destino.sede.id = :sedeId")
    Page<Traspaso> findBySedeId(@Param("sedeId") Long sedeId, Pageable pageable);
}