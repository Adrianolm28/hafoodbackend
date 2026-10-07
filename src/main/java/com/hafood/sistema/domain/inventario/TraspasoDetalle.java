package com.hafood.sistema.domain.inventario;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "traspaso_detalles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TraspasoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traspaso_id", nullable = false)
    private Traspaso traspaso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;
}