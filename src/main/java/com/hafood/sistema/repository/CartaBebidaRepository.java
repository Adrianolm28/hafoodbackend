package com.hafood.sistema.repository;

import com.hafood.sistema.domain.carta.CartaBebida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartaBebidaRepository extends JpaRepository<CartaBebida, Long> {

    List<CartaBebida> findByCartaId(Long cartaId);

    @Query("select cb from CartaBebida cb join fetch cb.bebida b left join fetch b.categoria "
            + "where cb.carta.id = :cartaId and cb.habilitado = true and b.activo = true")
    List<CartaBebida> findVisibles(@Param("cartaId") Long cartaId);

    @Query("select cb from CartaBebida cb join fetch cb.bebida b left join fetch b.categoria "
            + "where cb.carta.id = :cartaId and b.id = :bebidaId and cb.habilitado = true and b.activo = true")
    Optional<CartaBebida> findVisible(@Param("cartaId") Long cartaId, @Param("bebidaId") Long bebidaId);
}
