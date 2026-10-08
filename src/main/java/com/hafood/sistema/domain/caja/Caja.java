package com.hafood.sistema.domain.caja;

import com.hafood.sistema.domain.estructura.Sede;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cajas",
        uniqueConstraints = @UniqueConstraint(name = "uk_caja_sesion_abierta", columnNames = "sesion_abierta_id"),
        indexes = @Index(name = "idx_cajas_sede", columnList = "sede_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "sesion_abierta_id")
    private Long sesionAbiertaId;

    @Column(name = "creada_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadaEn = Instant.now();
}