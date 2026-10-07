package com.hafood.sistema.repository;

import com.hafood.sistema.domain.estructura.Mesa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findBySedeIdOrderByIdAsc(Long sedeId);

    List<Mesa> findBySedeIdAndActivaTrueOrderByIdAsc(Long sedeId);

    boolean existsBySeccionId(Long seccionId);

    boolean existsBySeccionIdAndActivaTrue(Long seccionId);

    boolean existsBySedeIdAndNombreIgnoreCaseAndActivaTrue(Long sedeId, String nombre);

    boolean existsBySedeIdAndNombreIgnoreCaseAndActivaTrueAndIdNot(Long sedeId, String nombre, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.seccion.id = :seccionId")
    List<Mesa> findBySeccionIdForUpdate(@Param("seccionId") Long seccionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.id = :id")
    Optional<Mesa> findByIdForUpdate(@Param("id") Long id);
}