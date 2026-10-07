package com.hafood.sistema.domain.carta;

import com.hafood.sistema.domain.estructura.Sede;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cartas", uniqueConstraints = @UniqueConstraint(name = "uk_carta_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, updatable = false, length = 12)
    private String codigo;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "mostrar_imagenes", nullable = false)
    @Builder.Default
    private boolean mostrarImagenes = false;

    @Column(name = "fondo_movil", length = 120)
    private String fondoMovil;

    @Column(name = "fondo_escritorio", length = 120)
    private String fondoEscritorio;

    @Column(length = 120)
    private String logo;

    @Column(name = "creada_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadaEn = Instant.now();
}
