package com.hafood.sistema.repository;

import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.pos.CuentaLinea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CuentaLineaRepository extends JpaRepository<CuentaLinea, Long> {

    @Query("select l from CuentaLinea l join fetch l.agregadaPor where l.cuenta.id = :cuentaId order by l.id")
    List<CuentaLinea> findByCuentaIdConUsuario(@Param("cuentaId") Long cuentaId);

    Optional<CuentaLinea> findByIdAndCuentaId(Long id, Long cuentaId);

    @Query("select l from CuentaLinea l join fetch l.cuenta c join fetch c.mozo left join fetch l.comanda "
            + "where c.sede.id = :sedeId and l.tipo = :tipo and l.estado in :estados "
            + "and c.estado in :estadosCuenta order by l.enviadaEn, l.id")
    List<CuentaLinea> findParaEstacion(@Param("sedeId") Long sedeId,
                                       @Param("tipo") TipoCategoria tipo,
                                       @Param("estados") Collection<EstadoLinea> estados,
                                       @Param("estadosCuenta") Collection<EstadoCuenta> estadosCuenta);

    @Query("select l.cuenta.id, count(l) from CuentaLinea l "
            + "where l.cuenta.sede.id = :sedeId and l.estado = :estado group by l.cuenta.id")
    List<Object[]> contarPorCuenta(@Param("sedeId") Long sedeId, @Param("estado") EstadoLinea estado);

    @Query("select l.cuenta.id from CuentaLinea l where l.id = :id")
    Optional<Long> findCuentaIdByLineaId(@Param("id") Long id);
}