package com.hafood.sistema.repository;

import com.hafood.sistema.domain.caja.CajaMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {

    @Query("select m from CajaMovimiento m join fetch m.registradoPor where m.sesion.id = :sesionId order by m.id")
    List<CajaMovimiento> findBySesionId(@Param("sesionId") Long sesionId);

    Optional<CajaMovimiento> findByClaveIdempotencia(String claveIdempotencia);

    Optional<CajaMovimiento> findByIdAndSesionId(Long id, Long sesionId);

    boolean existsByAnulaMovimientoId(Long anulaMovimientoId);

    @Query("select m.metodo, m.marcaTarjeta, m.moneda, m.tipo, sum(m.monto) from CajaMovimiento m "
            + "where m.sesion.id = :sesionId group by m.metodo, m.marcaTarjeta, m.moneda, m.tipo")
    List<Object[]> sumarPorSesion(@Param("sesionId") Long sesionId);
}