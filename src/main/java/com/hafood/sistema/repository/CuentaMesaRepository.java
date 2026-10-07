package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.CuentaMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaMesaRepository extends JpaRepository<CuentaMesa, Long> {

    @Query("select cm from CuentaMesa cm join fetch cm.mesa join fetch cm.cuenta c join fetch c.mozo "
            + "where cm.hasta is null and c.sede.id = :sedeId")
    List<CuentaMesa> findVigentesBySedeId(@Param("sedeId") Long sedeId);

    @Query("select cm from CuentaMesa cm join fetch cm.cuenta c join fetch c.mozo "
            + "where cm.mesa.id = :mesaId and cm.hasta is null")
    Optional<CuentaMesa> findVigenteByMesaId(@Param("mesaId") Long mesaId);

    @Query("select cm.cuenta.id from CuentaMesa cm where cm.mesa.id = :mesaId and cm.hasta is null")
    Optional<Long> findCuentaIdVigenteByMesaId(@Param("mesaId") Long mesaId);

    @Query("select cm from CuentaMesa cm join fetch cm.mesa m join fetch m.seccion "
            + "where cm.cuenta.id = :cuentaId and cm.hasta is null order by cm.id")
    List<CuentaMesa> findVigentesByCuentaId(@Param("cuentaId") Long cuentaId);

    boolean existsByMesaIdAndHastaIsNull(Long mesaId);
}