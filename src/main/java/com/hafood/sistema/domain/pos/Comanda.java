package com.hafood.sistema.domain.pos;

import com.hafood.sistema.constant.EstacionComanda;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "comandas", indexes = @Index(name = "idx_comanda_cuenta", columnList = "cuenta_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstacionComanda estacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviada_por_id", nullable = false)
    private Usuario enviadaPor;

    @Column(name = "enviada_en", nullable = false)
    private Instant enviadaEn;
}