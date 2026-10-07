package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.constant.EstadoPrestamo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traspaso_id", nullable = false, unique = true)
    private Traspaso traspaso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPrestamo estado;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrestamoDetalle> detalles = new ArrayList<>();
}