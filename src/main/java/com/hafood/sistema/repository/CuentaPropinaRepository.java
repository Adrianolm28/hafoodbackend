package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.CuentaPropina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CuentaPropinaRepository extends JpaRepository<CuentaPropina, Long> {

    @Query("select p from CuentaPropina p join fetch p.registradoPor where p.cuenta.id = :cuentaId order by p.id")
    List<CuentaPropina> findByCuentaId(@Param("cuentaId") Long cuentaId);
}