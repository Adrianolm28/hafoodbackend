package com.hafood.sistema.domain.estructura;

import com.hafood.sistema.constant.TipoUbicacion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ubicaciones", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sede_id", "seccion_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoUbicacion tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}