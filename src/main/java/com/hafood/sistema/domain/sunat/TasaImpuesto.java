package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasas_impuesto", indexes = @Index(name = "idx_tasa_regimen_tipo", columnList = "regimen, tipo, vigente_desde"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TasaImpuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegimenTributario regimen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private TipoImpuesto tipo;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();
}