package com.hafood.sistema.domain.pos;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "operaciones_procesadas", uniqueConstraints = @UniqueConstraint(name = "uk_operacion_clave", columnNames = "clave"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperacionProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String clave;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(name = "creada_en", nullable = false)
    @Builder.Default
    private Instant creadaEn = Instant.now();
}