package com.hafood.sistema.repository;

import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.domain.sunat.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long>, JpaSpecificationExecutor<Comprobante> {

    Optional<Comprobante> findByCuentaVigenteId(Long cuentaId);

    boolean existsByRegimenAndFechaEmisionGreaterThanEqual(RegimenTributario regimen, LocalDate fecha);
}