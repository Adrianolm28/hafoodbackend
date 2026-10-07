package com.hafood.sistema.domain.estructura;

import com.hafood.sistema.constant.FormaMesa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaMesa forma;

    @Column(name = "pos_x", nullable = false)
    private Integer posX;

    @Column(name = "pos_y", nullable = false)
    private Integer posY;

    @Column(nullable = false)
    private Integer ancho;

    @Column(nullable = false)
    private Integer alto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean temporal = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueada = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}