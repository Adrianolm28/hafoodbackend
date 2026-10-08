package com.hafood.sistema.domain.caja;

import com.hafood.sistema.domain.estructura.Seccion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caja_secciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_caja_seccion", columnNames = "seccion_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;
}