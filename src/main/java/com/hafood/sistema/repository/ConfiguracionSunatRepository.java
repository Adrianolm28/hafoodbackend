package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfiguracionSunatRepository extends JpaRepository<ConfiguracionSunat, Long> {

    Optional<ConfiguracionSunat> findByClaveUnica(String claveUnica);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracionSunat c where c.claveUnica = :clave")
    Optional<ConfiguracionSunat> findByClaveUnicaForUpdate(@Param("clave") String clave);
}