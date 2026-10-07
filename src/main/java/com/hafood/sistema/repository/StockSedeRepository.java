package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.StockSede;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockSedeRepository extends JpaRepository<StockSede, Long> {

    Optional<StockSede> findByInsumoIdAndSedeId(Long insumoId, Long sedeId);

    Page<StockSede> findBySedeId(Long sedeId, Pageable pageable);
}