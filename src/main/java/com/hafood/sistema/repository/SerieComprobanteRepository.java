package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.SerieComprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Long> {

    List<SerieComprobante> findBySedeId(Long sedeId);

    boolean existsBySerie(String serie);
}