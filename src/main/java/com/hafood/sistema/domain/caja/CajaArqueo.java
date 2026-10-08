package com.hafood.sistema.domain.caja;

import com.hafood.sistema.constant.TipoArqueo;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "caja_arqueos", indexes = @Index(name = "idx_caja_arqueo_sesion", columnList = "sesion_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaArqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private CajaSesion sesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArqueo tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private Usuario realizadoPor;

    @Column(name = "realizado_en", nullable = false)
    private Instant realizadoEn;

    @Column(length = 300)
    private String observacion;

    @Column(name = "motivo_diferencia", length = 300)
    private String motivoDiferencia;

    @Column(name = "hay_diferencia", nullable = false)
    private boolean hayDiferencia;
}