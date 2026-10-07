package com.hafood.sistema.repository;

import com.hafood.sistema.domain.carta.CartaCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartaCategoriaRepository extends JpaRepository<CartaCategoria, Long> {

    List<CartaCategoria> findByCartaId(Long cartaId);

    @Query("select cc from CartaCategoria cc join fetch cc.categoria c "
            + "where cc.carta.id = :cartaId and cc.habilitada = true and c.activo = true")
    List<CartaCategoria> findVisibles(@Param("cartaId") Long cartaId);

    @Query("select count(cc) > 0 from CartaCategoria cc where cc.carta.id = :cartaId "
            + "and cc.categoria.id = :categoriaId and cc.habilitada = true and cc.categoria.activo = true")
    boolean estaHabilitada(@Param("cartaId") Long cartaId, @Param("categoriaId") Long categoriaId);
}
