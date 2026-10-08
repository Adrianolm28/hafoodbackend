package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.AlertaStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    List<AlertaStock> findBySedeIdOrderByCreadaEnDesc(Long sedeId, Pageable pageable);

    List<AlertaStock> findBySedeIdAndRevisadaFalseOrderByCreadaEnDesc(Long sedeId, Pageable pageable);
}