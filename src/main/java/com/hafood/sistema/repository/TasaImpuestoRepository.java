package com.hafood.sistema.repository;

import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;
import com.hafood.sistema.domain.sunat.TasaImpuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TasaImpuestoRepository extends JpaRepository<TasaImpuesto, Long> {

    @Query("select t from TasaImpuesto t where t.regimen = :regimen and t.tipo = :tipo "
            + "and t.vigenteDesde <= :fecha and (t.vigenteHasta is null or t.vigenteHasta >= :fecha)")
    List<TasaImpuesto> findVigentes(@Param("regimen") RegimenTributario regimen,
                                    @Param("tipo") TipoImpuesto tipo,
                                    @Param("fecha") LocalDate fecha);

    List<TasaImpuesto> findByRegimenAndTipoOrderByVigenteDesde(RegimenTributario regimen, TipoImpuesto tipo);

    List<TasaImpuesto> findAllByOrderByRegimenAscTipoAscVigenteDesdeAsc();
}