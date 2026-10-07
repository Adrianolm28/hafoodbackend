package com.hafood.sistema.repository;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.catalogo.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Page<Categoria> findByTipo(TipoCategoria tipo, Pageable pageable);

    Page<Categoria> findByActivoTrue(Pageable pageable);

    Page<Categoria> findByActivoTrueAndTipo(TipoCategoria tipo, Pageable pageable);

    List<Categoria> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCaseAndTipo(String nombre, TipoCategoria tipo);

    boolean existsByNombreIgnoreCaseAndTipoAndIdNot(String nombre, TipoCategoria tipo, Long id);
}
