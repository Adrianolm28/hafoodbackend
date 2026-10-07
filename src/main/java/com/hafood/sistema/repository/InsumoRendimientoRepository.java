package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.InsumoRendimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsumoRendimientoRepository extends JpaRepository<InsumoRendimiento, Long> {

    Optional<InsumoRendimiento> findByInsumoOrigenIdAndInsumoDestinoId(Long origenId, Long destinoId);

    List<InsumoRendimiento> findAllByOrderByIdAsc();
}