package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.ComprobanteLinea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComprobanteLineaRepository extends JpaRepository<ComprobanteLinea, Long> {

    List<ComprobanteLinea> findByComprobanteIdOrderByItem(Long comprobanteId);
}