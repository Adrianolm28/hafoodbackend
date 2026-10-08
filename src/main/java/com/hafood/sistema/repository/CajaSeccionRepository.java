package com.hafood.sistema.repository;

import com.hafood.sistema.domain.caja.CajaSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CajaSeccionRepository extends JpaRepository<CajaSeccion, Long> {

    @Query("select cs from CajaSeccion cs join fetch cs.seccion where cs.caja.id in :cajaIds order by cs.seccion.nombre")
    List<CajaSeccion> findByCajaIds(@Param("cajaIds") Collection<Long> cajaIds);

    @Query("select cs from CajaSeccion cs join fetch cs.caja where cs.seccion.id = :seccionId")
    Optional<CajaSeccion> findBySeccionIdConCaja(@Param("seccionId") Long seccionId);

    boolean existsByCajaId(Long cajaId);

    @Modifying(flushAutomatically = true)
    @Query("delete from CajaSeccion cs where cs.caja.id = :cajaId")
    void eliminarPorCajaId(@Param("cajaId") Long cajaId);
}