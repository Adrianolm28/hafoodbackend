package com.hafood.sistema.domain.barra;

import com.hafood.sistema.domain.catalogo.Categoria;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bebidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bebida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String nombre;

    @Column(length = 120)
    private String descripcion;

    @Column(name = "precio_venta")
    private BigDecimal precioVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(length = 120)
    private String imagen;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
