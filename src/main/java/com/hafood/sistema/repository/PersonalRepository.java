package com.hafood.sistema.repository;

import com.hafood.sistema.domain.estructura.Personal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    Page<Personal> findBySedeId(Long sedeId, Pageable pageable);

    Page<Personal> findBySedeIdAndActivoTrue(Long sedeId, Pageable pageable);

    List<Personal> findBySedeIdAndActivoTrueOrderByCodigoAsc(Long sedeId);

    boolean existsBySedeIdAndCodigo(Long sedeId, Integer codigo);

    boolean existsBySedeIdAndCodigoAndIdNot(Long sedeId, Integer codigo, Long id);

    @Query("select max(p.codigo) from Personal p where p.sede.id = :sedeId")
    Integer findMaxCodigoBySedeId(@Param("sedeId") Long sedeId);

    @Query("select count(s) > 0 from Seccion s where s.jefeMozo.id = :personalId or s.jefeBartender.id = :personalId")
    boolean esJefeDeSeccion(@Param("personalId") Long personalId);
}