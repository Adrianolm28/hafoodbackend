package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.*;
import com.hafood.sistema.constant.TipoTraspaso;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "traspasos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Traspaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = false)
    private Ubicacion origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private Ubicacion destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    @Builder.Default
    private TipoTraspaso tipo = TipoTraspaso.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id")
    private Prestamo prestamo;

    @OneToMany(mappedBy = "traspaso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TraspasoDetalle> detalles = new ArrayList<>();
}