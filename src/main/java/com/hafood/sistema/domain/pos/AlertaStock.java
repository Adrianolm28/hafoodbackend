package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.TipoAlertaStock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alertas_stock", indexes = @Index(name = "idx_alerta_sede_revisada", columnList = "sede_id, revisada, creada_en"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(name = "linea_id")
    private Long lineaId;

    @Column(name = "insumo_id")
    private Long insumoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoAlertaStock tipo;

    @Column(name = "cantidad_faltante", precision = 14, scale = 3)
    private BigDecimal cantidadFaltante;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "creada_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadaEn = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean revisada = false;

    @Column(name = "revisada_por_nombre", length = 60)
    private String revisadaPorNombre;

    @Column(name = "revisada_en")
    private Instant revisadaEn;
}