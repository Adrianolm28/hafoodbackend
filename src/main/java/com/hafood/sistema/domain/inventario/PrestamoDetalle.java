package com.hafood.sistema.domain.inventario;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prestamo_detalles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PrestamoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(name = "cantidad_prestada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadPrestada;

    @Column(name = "cantidad_devuelta", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadDevuelta;
}