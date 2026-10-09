package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.OperacionProcesada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacionProcesadaRepository extends JpaRepository<OperacionProcesada, Long> {

    boolean existsByClave(String clave);

    Optional<OperacionProcesada> findByClave(String clave);
}