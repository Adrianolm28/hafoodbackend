package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.TipoDescuento;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuenta_descuentos", indexes = @Index(name = "idx_descuento_cuenta", columnList = "cuenta_id, activo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_id")
    private CuentaLinea linea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoDescuento tipo;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "cantidad_cortesia")
    private Integer cantidadCortesia;

    @Column(name = "monto_antes", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoAntes = BigDecimal.ZERO;

    @Column(name = "monto_descuento", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "monto_despues", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoDespues = BigDecimal.ZERO;

    @Column(nullable = false, length = 200)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecutado_por_id", nullable = false)
    private Usuario ejecutadoPor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autorizado_por_id", nullable = false)
    private Usuario autorizadoPor;

    @Column(name = "auto_autorizado", nullable = false)
    private boolean autoAutorizado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "quitado_en")
    private Instant quitadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quitado_por_id")
    private Usuario quitadoPor;

    @Column(name = "motivo_quitado", length = 200)
    private String motivoQuitado;
}