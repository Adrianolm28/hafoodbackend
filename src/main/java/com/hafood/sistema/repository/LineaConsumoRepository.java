package com.hafood.sistema.repository;

import com.hafood.sistema.constant.EstadoConsumo;
import com.hafood.sistema.domain.pos.LineaConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LineaConsumoRepository extends JpaRepository<LineaConsumo, Long> {

    @Query("select c from LineaConsumo c join fetch c.insumo join fetch c.ubicacion "
            + "where c.linea.id = :lineaId and c.estado = :estado order by c.ubicacion.id, c.insumo.id, c.id")
    List<LineaConsumo> findByLineaIdYEstado(@Param("lineaId") Long lineaId, @Param("estado") EstadoConsumo estado);
}