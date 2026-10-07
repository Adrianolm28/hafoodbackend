package com.hafood.sistema.repository;

import com.hafood.sistema.constant.TipoUbicacion;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    List<Ubicacion> findBySedeIdOrderByTipoAscNombreAsc(Long sedeId);

    Optional<Ubicacion> findFirstBySedeIdAndTipo(Long sedeId, TipoUbicacion tipo);

    Optional<Ubicacion> findBySeccionId(Long seccionId);

    @Query("select s from Seccion s where s.sede.id = :sedeId")
    List<Seccion> findSeccionesBySedeId(@Param("sedeId") Long sedeId);
}