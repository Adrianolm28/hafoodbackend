package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuenta_pagos", indexes = {
        @Index(name = "idx_cuenta_pago_cuenta", columnList = "cuenta_id"),
        @Index(name = "idx_cuenta_pago_sesion", columnList = "caja_sesion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_sesion_id", nullable = false)
    private CajaSesion cajaSesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "marca_tarjeta", length = 12)
    private MarcaTarjeta marcaTarjeta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recibido;

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 2)
    private BigDecimal tipoCambio;

    @Column(name = "recibido_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal recibidoPen;

    @Column(name = "aplicado_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal aplicadoPen;

    @Column(name = "vuelto_monto", precision = 12, scale = 2)
    private BigDecimal vueltoMonto;

    @Enumerated(EnumType.STRING)
    @Column(name = "vuelto_moneda", length = 3)
    private Moneda vueltoMoneda;

    @Column(name = "vuelto_pen", precision = 12, scale = 2)
    private BigDecimal vueltoPen;

    @Column(length = 60)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();

    @Column(name = "movimiento_id")
    private Long movimientoId;

    @Column(name = "movimiento_vuelto_id")
    private Long movimientoVueltoId;
}