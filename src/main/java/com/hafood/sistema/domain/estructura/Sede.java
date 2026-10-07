package com.hafood.sistema.domain.estructura;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sedes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    private String direccion;
}