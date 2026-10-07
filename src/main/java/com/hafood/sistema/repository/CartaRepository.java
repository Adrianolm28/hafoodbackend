package com.hafood.sistema.repository;

import com.hafood.sistema.domain.carta.Carta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartaRepository extends JpaRepository<Carta, Long> {

    @Query("select c from Carta c join fetch c.sede where c.sede.id = :sedeId order by lower(c.nombre)")
    List<Carta> listarPorSede(@Param("sedeId") Long sedeId);

    @Query("select c from Carta c join fetch c.sede where c.codigo = :codigo")
    Optional<Carta> buscarPorCodigo(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsBySedeIdAndNombreIgnoreCase(Long sedeId, String nombre);

    boolean existsBySedeIdAndNombreIgnoreCaseAndIdNot(Long sedeId, String nombre, Long id);

}
