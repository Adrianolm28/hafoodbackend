package com.hafood.sistema.repository;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.domain.sunat.Comprobante;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long>, JpaSpecificationExecutor<Comprobante> {

    Optional<Comprobante> findByCuentaVigenteId(Long cuentaId);

    boolean existsByRegimenAndFechaEmisionGreaterThanEqual(RegimenTributario regimen, LocalDate fecha);

    @Query("select c.id from Comprobante c where c.sede.id = :sedeId and c.estadoSunat in :estados "
            + "order by c.fechaEmision asc, c.id asc")
    List<Long> findIdsParaEnviar(@Param("sedeId") Long sedeId,
                                 @Param("estados") Collection<EstadoSunat> estados,
                                 Pageable pageable);
}