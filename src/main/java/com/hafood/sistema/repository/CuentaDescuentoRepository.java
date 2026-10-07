package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.CuentaDescuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaDescuentoRepository extends JpaRepository<CuentaDescuento, Long> {

    @Query("select d from CuentaDescuento d join fetch d.ejecutadoPor join fetch d.autorizadoPor "
            + "left join fetch d.linea where d.cuenta.id = :cuentaId and d.activo = true order by d.id")
    List<CuentaDescuento> findActivosByCuentaId(@Param("cuentaId") Long cuentaId);

    List<CuentaDescuento> findByCuentaId(Long cuentaId);

    Optional<CuentaDescuento> findByIdAndCuentaId(Long id, Long cuentaId);

    boolean existsByLineaIdAndActivoTrue(Long lineaId);

    boolean existsByCuentaIdAndLineaIsNullAndActivoTrue(Long cuentaId);
}