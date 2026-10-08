package com.hafood.sistema.repository;

import com.hafood.sistema.domain.caja.CajaArqueoLinea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CajaArqueoLineaRepository extends JpaRepository<CajaArqueoLinea, Long> {

    List<CajaArqueoLinea> findByArqueoIdInOrderByIdAsc(Collection<Long> arqueoIds);
}