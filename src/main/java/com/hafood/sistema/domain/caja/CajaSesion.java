package com.hafood.sistema.domain.caja;

import com.hafood.sistema.constant.EstadoCajaSesion;
import com.hafood.sistema.constant.TipoArqueo;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caja_sesiones", indexes = {
        @Index(name = "idx_caja_sesion_caja", columnList = "caja_id, estado"),
        @Index(name = "idx_caja_sesion_sede", columnList = "sede_id, abierta_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoCajaSesion estado = EstadoCajaSesion.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abierta_por_id", nullable = false)
    private Usuario abiertaPor;

    @Column(name = "abierta_en", nullable = false)
    @Builder.Default
    private Instant abiertaEn = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrada_por_id")
    private Usuario cerradaPor;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cierre", length = 20)
    private TipoArqueo tipoCierre;

    @Column(name = "entregado_a", length = 100)
    private String entregadoA;

    @Column(name = "turno_anterior_id")
    private Long turnoAnteriorId;

    @Column(name = "diferencia_pendiente", nullable = false)
    @Builder.Default
    private boolean diferenciaPendiente = false;

    @Column(name = "diferencia_pen", precision = 12, scale = 2)
    private BigDecimal diferenciaPen;

    @Column(name = "lineas_con_diferencia")
    private Integer lineasConDiferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisada_por_id")
    private Usuario revisadaPor;

    @Column(name = "revisada_en")
    private Instant revisadaEn;

    @Column(name = "comentario_revision", length = 300)
    private String comentarioRevision;
}