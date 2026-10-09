package com.hafood.sistema.domain.sunat;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "comprobante_lineas", indexes = @Index(name = "idx_comprobante_linea_comprobante", columnList = "comprobante_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(nullable = false)
    private int item;

    @Column(name = "cuenta_linea_id")
    private Long cuentaLineaId;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(name = "unidad_medida", nullable = false, length = 3)
    private String unidadMedida;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "base_imponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseImponible;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ipm;
}