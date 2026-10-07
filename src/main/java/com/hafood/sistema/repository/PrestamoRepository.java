package com.hafood.sistema.repository;

import com.hafood.sistema.domain.inventario.Prestamo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    @Query("""
            select p from Prestamo p
            where ((:rol = 'PRESTAMISTA' and p.traspaso.origen.sede.id = :sedeId)
                or (:rol = 'PRESTATARIO' and p.traspaso.destino.sede.id = :sedeId)
                or (:rol = 'TODOS' and (p.traspaso.origen.sede.id = :sedeId or p.traspaso.destino.sede.id = :sedeId)))
              and (:soloPendientes = false or p.estado <> com.hafood.sistema.constant.EstadoPrestamo.DEVUELTO)
            """)
    Page<Prestamo> buscar(@Param("sedeId") Long sedeId,
                          @Param("rol") String rol,
                          @Param("soloPendientes") boolean soloPendientes,
                          Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> findForUpdate(@Param("id") Long id);
}