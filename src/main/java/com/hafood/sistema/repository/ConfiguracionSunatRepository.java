package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionSunatRepository extends JpaRepository<ConfiguracionSunat, Long> {

    Optional<ConfiguracionSunat> findByClaveUnica(String claveUnica);
}