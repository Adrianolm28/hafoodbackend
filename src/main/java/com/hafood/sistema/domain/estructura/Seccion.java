package com.hafood.sistema.domain.estructura;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "secciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_mozo_id")
    private Personal jefeMozo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_bartender_id")
    private Personal jefeBartender;

    @Column
    @Builder.Default
    private Boolean activo = true;

    @Column
    private Integer orden;
}