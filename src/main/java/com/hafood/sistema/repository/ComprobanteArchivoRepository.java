package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.ComprobanteArchivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobanteArchivoRepository extends JpaRepository<ComprobanteArchivo, Long> {

    Optional<ComprobanteArchivo> findByComprobanteId(Long comprobanteId);
}