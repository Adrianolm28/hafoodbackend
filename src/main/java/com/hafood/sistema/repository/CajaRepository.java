package com.hafood.sistema.repository;

import com.hafood.sistema.domain.caja.Caja;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caja c where c.id = :id")
    Optional<Caja> findByIdForUpdate(@Param("id") Long id);

    List<Caja> findBySedeIdOrderByNombreAsc(Long sedeId);

    List<Caja> findBySedeIdAndActivaTrueOrderByNombreAsc(Long sedeId);

    boolean existsBySedeIdAndNombreIgnoreCase(Long sedeId, String nombre);

    boolean existsBySedeIdAndNombreIgnoreCaseAndIdNot(Long sedeId, String nombre, Long id);
}