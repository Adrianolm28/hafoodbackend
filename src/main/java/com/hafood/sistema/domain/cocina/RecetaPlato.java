package com.hafood.sistema.domain.cocina;

import com.hafood.sistema.domain.inventario.Insumo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recetas_platos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RecetaPlato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;
}