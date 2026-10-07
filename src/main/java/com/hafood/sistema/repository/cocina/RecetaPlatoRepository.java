package com.hafood.sistema.repository.cocina;

import com.hafood.sistema.domain.cocina.RecetaPlato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecetaPlatoRepository extends JpaRepository<RecetaPlato, Long> {

    Page<RecetaPlato> findByPlatoId(Long platoId, Pageable pageable);

    boolean existsByPlatoIdAndInsumoId(Long platoId, Long insumoId);

    boolean existsByInsumoId(Long insumoId);

    void deleteByPlatoId(Long platoId);
}