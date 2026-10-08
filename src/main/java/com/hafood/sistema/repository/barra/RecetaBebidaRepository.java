package com.hafood.sistema.repository.barra;

import com.hafood.sistema.domain.barra.RecetaBebida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecetaBebidaRepository extends JpaRepository<RecetaBebida, Long> {

    Page<RecetaBebida> findByBebidaId(Long bebidaId, Pageable pageable);

    boolean existsByBebidaIdAndInsumoId(Long bebidaId, Long insumoId);

    boolean existsByInsumoId(Long insumoId);

    void deleteByBebidaId(Long bebidaId);

    List<RecetaBebida> findAllByBebidaId(Long bebidaId);
}