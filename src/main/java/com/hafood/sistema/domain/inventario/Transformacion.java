package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transformaciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Transformacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_origen_id", nullable = false)
    private Insumo insumoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_destino_id", nullable = false)
    private Insumo insumoDestino;

    @Column(name = "cantidad_origen", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadOrigen;

    @Column(name = "cantidad_destino", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadDestino;

    @Column(nullable = false)
    private boolean estimada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;
}