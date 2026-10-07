package com.hafood.sistema.domain.carta;

import com.hafood.sistema.domain.catalogo.Categoria;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carta_categorias", uniqueConstraints = @UniqueConstraint(name = "uk_carta_categoria", columnNames = {"carta_id", "categoria_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carta_id", nullable = false)
    private Carta carta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    @Builder.Default
    private boolean habilitada = true;

    @Column(nullable = false)
    @Builder.Default
    private int orden = 0;
}
