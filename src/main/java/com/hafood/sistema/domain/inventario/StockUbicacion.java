package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.domain.estructura.Ubicacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_ubicacion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"insumo_id", "ubicacion_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StockUbicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(name = "cantidad_actual", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadActual;
}