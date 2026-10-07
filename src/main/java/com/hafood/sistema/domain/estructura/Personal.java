package com.hafood.sistema.domain.estructura;

import com.hafood.sistema.constant.TipoPersonal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "personal",
        uniqueConstraints = @UniqueConstraint(name = "uk_personal_sede_codigo", columnNames = {"sede_id", "codigo"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false)
    private Integer codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPersonal tipo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}