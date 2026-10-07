package com.hafood.sistema.repository;

import com.hafood.sistema.domain.barra.Bebida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BebidaRepository extends JpaRepository<Bebida, Long> {

    Page<Bebida> findAllByActivo(Boolean activo, Pageable pageable);

    @Query("select b from Bebida b left join fetch b.categoria where b.activo = true order by b.nombre")
    List<Bebida> findActivosConCategoria();
}
