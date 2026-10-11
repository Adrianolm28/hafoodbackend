package com.hafood.sistema.repository;

import com.hafood.sistema.domain.sunat.ComprobanteArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComprobanteArchivoRepository extends JpaRepository<ComprobanteArchivo, Long> {

    Optional<ComprobanteArchivo> findByComprobanteId(Long comprobanteId);

    @Query("select a.comprobante.id, a.firmado, a.codigoRespuesta, a.mensajeRespuesta, a.errorGeneracion, "
            + "a.intentos, case when a.cdr is not null then true else false end "
            + "from ComprobanteArchivo a where a.comprobante.id in :ids")
    List<Object[]> findEstadosPorComprobanteIds(@Param("ids") Collection<Long> ids);
}