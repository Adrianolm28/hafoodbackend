package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.SedeSunat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SedeSunatRepository extends JpaRepository<SedeSunat, Long> {

    Optional<SedeSunat> findBySedeId(Long sedeId);
}