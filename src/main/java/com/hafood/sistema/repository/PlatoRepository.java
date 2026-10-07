package com.hafood.sistema.repository;

import com.hafood.sistema.domain.cocina.Plato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Long> {

    Page<Plato> findAllByActivo(Boolean activo, Pageable pageable);

    @Query("select p from Plato p left join fetch p.categoria where p.activo = true order by p.nombre")
    List<Plato> findActivosConCategoria();
}
