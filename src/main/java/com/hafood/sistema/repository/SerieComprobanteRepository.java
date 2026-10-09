package com.hafood.sistema.repository;

import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.sunat.SerieComprobante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Long> {

    List<SerieComprobante> findBySedeId(Long sedeId);

    boolean existsBySerie(String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SerieComprobante s where s.sede.id = :sedeId and s.tipo = :tipo")
    Optional<SerieComprobante> findBySedeIdAndTipoForUpdate(@Param("sedeId") Long sedeId,
                                                            @Param("tipo") TipoComprobante tipo);
}