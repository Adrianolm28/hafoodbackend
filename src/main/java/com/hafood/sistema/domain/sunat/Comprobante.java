package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.pos.Cuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "comprobantes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comprobante_cuenta_vigente", columnNames = "cuenta_vigente_id"),
                @UniqueConstraint(name = "uk_comprobante_numero", columnNames = {"tipo", "serie", "correlativo"})
        },
        indexes = {
                @Index(name = "idx_comprobante_sede_fecha", columnList = "sede_id, fecha_emision"),
                @Index(name = "idx_comprobante_estado", columnList = "estado_sunat"),
                @Index(name = "idx_comprobante_cuenta", columnList = "cuenta_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "cuenta_vigente_id")
    private Long cuentaVigenteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoComprobante tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private long correlativo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "emitido_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant emitidoEn = Instant.now();

    @Column(name = "emitido_por_id", nullable = false)
    private Long emitidoPorId;

    @Column(name = "emitido_por_nombre", nullable = false, length = 100)
    private String emitidoPorNombre;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moneda = "PEN";

    @Column(name = "ruc_emisor", nullable = false, length = 11)
    private String rucEmisor;

    @Column(name = "razon_social_emisor", nullable = false, length = 200)
    private String razonSocialEmisor;

    @Column(name = "nombre_comercial_emisor", length = 200)
    private String nombreComercialEmisor;

    @Column(name = "codigo_establecimiento", nullable = false, length = 4)
    private String codigoEstablecimiento;

    @Column(name = "direccion_fiscal", nullable = false, length = 250)
    private String direccionFiscal;

    @Column(nullable = false, length = 6)
    private String ubigeo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AmbienteSunat ambiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegimenTributario regimen;

    @Column(name = "igv_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal igvPorcentaje;

    @Column(name = "ipm_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ipmPorcentaje;

    @Embedded
    private ComprobanteCliente cliente;

    @Column(name = "op_gravada", nullable = false, precision = 12, scale = 2)
    private BigDecimal opGravada;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ipm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sunat", nullable = false, length = 30)
    @Builder.Default
    private EstadoSunat estadoSunat = EstadoSunat.PENDIENTE;

    @Version
    private Long version;
}