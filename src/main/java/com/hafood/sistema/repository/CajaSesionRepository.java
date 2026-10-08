package com.hafood.sistema.repository;

import com.hafood.sistema.constant.EstadoCajaSesion;
import com.hafood.sistema.domain.caja.CajaSesion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CajaSesionRepository extends JpaRepository<CajaSesion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CajaSesion s where s.id = :id")
    Optional<CajaSesion> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from CajaSesion s where s.id = :id")
    Optional<CajaSesion> findByIdForShare(@Param("id") Long id);

    @Query("select s.sede.id from CajaSesion s where s.id = :id")
    Optional<Long> findSedeIdById(@Param("id") Long id);

    boolean existsByAbiertaPorIdAndEstado(Long usuarioId, EstadoCajaSesion estado);

    @Query("select s from CajaSesion s join fetch s.caja join fetch s.abiertaPor "
            + "left join fetch s.cerradaPor left join fetch s.revisadaPor "
            + "where s.sede.id = :sedeId and s.abiertaEn >= :desde and s.abiertaEn < :hasta "
            + "order by s.abiertaEn desc")
    List<CajaSesion> findHistorial(@Param("sedeId") Long sedeId,
                                   @Param("desde") Instant desde,
                                   @Param("hasta") Instant hasta,
                                   Pageable pageable);
}