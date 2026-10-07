package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.domain.estructura.Sede;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_sede", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"insumo_id", "sede_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StockSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "cantidad_actual", nullable = false)
    private BigDecimal cantidadActual;
}