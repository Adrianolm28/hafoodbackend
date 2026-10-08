package com.hafood.sistema.repository;

import com.hafood.sistema.domain.caja.CajaArqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CajaArqueoRepository extends JpaRepository<CajaArqueo, Long> {

    @Query("select a from CajaArqueo a join fetch a.realizadoPor where a.sesion.id = :sesionId order by a.id")
    List<CajaArqueo> findBySesionId(@Param("sesionId") Long sesionId);
}