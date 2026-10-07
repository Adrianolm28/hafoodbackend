package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.constant.UnidadIngreso;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_detalles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(name = "cantidad_ingresada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadIngresada;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_ingreso", nullable = false, length = 15)
    private UnidadIngreso unidadIngreso;

    @Column(name = "cantidad_recibida", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadRecibida;

    @Column(name = "cantidad_esperada", precision = 14, scale = 3)
    private BigDecimal cantidadEsperada;

    @Column(name = "monto_pagado", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 6)
    private BigDecimal costoUnitario;
}