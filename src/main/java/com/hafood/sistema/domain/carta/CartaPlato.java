package com.hafood.sistema.domain.carta;

import com.hafood.sistema.domain.cocina.Plato;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "carta_platos", uniqueConstraints = @UniqueConstraint(name = "uk_carta_plato", columnNames = {"carta_id", "plato_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaPlato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carta_id", nullable = false)
    private Carta carta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    @Column(nullable = false)
    @Builder.Default
    private boolean habilitado = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean agotado = false;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    @Builder.Default
    private int orden = 0;
}
