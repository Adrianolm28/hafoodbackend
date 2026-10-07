package com.hafood.sistema.domain.pos;

import com.hafood.sistema.domain.estructura.Mesa;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "cuenta_mesas",
        uniqueConstraints = @UniqueConstraint(name = "uk_cuenta_mesa_vigente", columnNames = "mesa_vigente_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @Column(nullable = false)
    private Instant desde;

    private Instant hasta;

    @Column(name = "mesa_vigente_id")
    private Long mesaVigenteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movida_por_id")
    private Usuario movidaPor;
}