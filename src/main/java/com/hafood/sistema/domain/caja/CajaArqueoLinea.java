package com.hafood.sistema.domain.caja;

import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "caja_arqueo_lineas", indexes = @Index(name = "idx_caja_arqueo_linea", columnList = "arqueo_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaArqueoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arqueo_id", nullable = false)
    private CajaArqueo arqueo;

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
    private BigDecimal esperado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal contado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 400)
    private String detalle;
}