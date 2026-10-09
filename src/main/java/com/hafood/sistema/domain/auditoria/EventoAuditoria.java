package com.hafood.sistema.domain.auditoria;

import com.hafood.sistema.constant.AccionAuditoria;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "eventos_auditoria",
        indexes = {
                @Index(name = "idx_auditoria_cuenta", columnList = "cuenta_id"),
                @Index(name = "idx_auditoria_sede_fecha", columnList = "sede_id, creado_en")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sede_id")
    private Long sedeId;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionAuditoria accion;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "usuario_nombre", nullable = false, length = 100)
    private String usuarioNombre;

    @Column(length = 1000)
    private String antes;

    @Column(length = 1000)
    private String despues;

    @Column(length = 200)
    private String motivo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();
}