package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.EstadoConsumo;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Insumo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "linea_consumos", indexes = @Index(name = "idx_linea_consumo_linea", columnList = "linea_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaConsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private CuentaLinea linea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private EstadoConsumo estado = EstadoConsumo.DESCONTADO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();
}