package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuenta_lineas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id")
    private Cuenta cuentaOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCategoria tipo;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(length = 200)
    private String nota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoLinea estado = EstadoLinea.BORRADOR;

    @Column(name = "carta_id")
    private Long cartaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agregada_por_id", nullable = false)
    private Usuario agregadaPor;

    @Column(name = "agregada_en", nullable = false)
    @Builder.Default
    private Instant agregadaEn = Instant.now();

    @Column(name = "descuento_linea", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuentoLinea = BigDecimal.ZERO;

    @Column(name = "descuento_cuenta", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuentoCuenta = BigDecimal.ZERO;

    @Column(name = "total_linea", precision = 12, scale = 2)
    private BigDecimal totalLinea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_id")
    private Comanda comanda;

    @Column(name = "enviada_en")
    private Instant enviadaEn;

    @Column(name = "preparacion_en")
    private Instant preparacionEn;

    @Column(name = "lista_en")
    private Instant listaEn;

    @Column(name = "entregada_en")
    private Instant entregadaEn;

    @Column(name = "anulada_en")
    private Instant anuladaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulada_por_id")
    private Usuario anuladaPor;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @Column(name = "anulacion_vista")
    @Builder.Default
    private Boolean anulacionVista = false;

    @Column(name = "merma_anulacion")
    private Boolean mermaAnulacion;
}