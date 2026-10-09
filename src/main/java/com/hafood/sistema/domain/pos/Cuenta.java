package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.estructura.Personal;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuentas", indexes = {
        @Index(name = "idx_cuentas_sede_estado", columnList = "sede_id, estado"),
        @Index(name = "idx_cuentas_padre", columnList = "cuenta_padre_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carta_id", nullable = false)
    private Carta carta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mozo_id", nullable = false)
    private Personal mozo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abierta_por_id", nullable = false)
    private Usuario abiertaPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoCuenta estado = EstadoCuenta.ABIERTA;

    private Integer comensales;

    @Column(length = 200)
    private String nota;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "abierta_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant abiertaEn = Instant.now();

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fusionada_en_id")
    private Cuenta fusionadaEn;

    @Version
    private Long version;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_sesion_id")
    private CajaSesion cajaSesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id")
    private Cuenta cuentaPadre;

    @Column(name = "total_precuenta", precision = 12, scale = 2)
    private BigDecimal totalPrecuenta;

    @Column(name = "precuenta_en")
    private Instant precuentaEn;
}