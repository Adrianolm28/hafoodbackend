package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.CuentaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaPagoRepository extends JpaRepository<CuentaPago, Long> {

    @Query("select p from CuentaPago p join fetch p.registradoPor where p.cuenta.id = :cuentaId order by p.id")
    List<CuentaPago> findByCuentaId(@Param("cuentaId") Long cuentaId);

    @Query("select sum(p.aplicadoPen) from CuentaPago p where p.cuenta.id = :cuentaId")
    BigDecimal sumarAplicado(@Param("cuentaId") Long cuentaId);

    boolean existsByCuentaId(Long cuentaId);

    default BigDecimal totalAplicado(Long cuentaId) {
        BigDecimal suma = sumarAplicado(cuentaId);
        return suma == null ? BigDecimal.ZERO.setScale(2) : suma;
    }
}