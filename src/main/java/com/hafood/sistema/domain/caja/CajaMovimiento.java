package com.hafood.sistema.domain.caja;

import com.hafood.sistema.constant.CategoriaCaja;
import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caja_movimientos",
        uniqueConstraints = @UniqueConstraint(name = "uk_caja_mov_clave", columnNames = "clave_idempotencia"),
        indexes = @Index(name = "idx_caja_mov_sesion", columnList = "sesion_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private CajaSesion sesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMovimientoCaja tipo;

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
    private BigDecimal monto;

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 2)
    private BigDecimal tipoCambio;

    @Column(name = "equivalente_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal equivalentePen;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaCaja categoria;

    @Column(length = 200)
    private String motivo;

    @Column(length = 100)
    private String beneficiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(length = 60)
    private String referencia;

    @Column(name = "anula_movimiento_id")
    private Long anulaMovimientoId;

    @Column(name = "clave_idempotencia", length = 64)
    private String claveIdempotencia;
}