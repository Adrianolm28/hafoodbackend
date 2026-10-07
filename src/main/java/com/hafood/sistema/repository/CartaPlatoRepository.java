package com.hafood.sistema.repository;

import com.hafood.sistema.domain.carta.CartaPlato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartaPlatoRepository extends JpaRepository<CartaPlato, Long> {

    List<CartaPlato> findByCartaId(Long cartaId);

    @Query("select cp from CartaPlato cp join fetch cp.plato p left join fetch p.categoria "
            + "where cp.carta.id = :cartaId and cp.habilitado = true and p.activo = true")
    List<CartaPlato> findVisibles(@Param("cartaId") Long cartaId);
    @Query("select cp from CartaPlato cp join fetch cp.plato p left join fetch p.categoria "
            + "where cp.carta.id = :cartaId and p.id = :platoId and cp.habilitado = true and p.activo = true")
    Optional<CartaPlato> findVisible(@Param("cartaId") Long cartaId, @Param("platoId") Long platoId);
}
