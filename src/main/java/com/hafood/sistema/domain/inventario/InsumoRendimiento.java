package com.hafood.sistema.domain.inventario;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "insumo_rendimientos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"insumo_origen_id", "insumo_destino_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InsumoRendimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_origen_id", nullable = false)
    private Insumo insumoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_destino_id", nullable = false)
    private Insumo insumoDestino;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal rendimiento;
}