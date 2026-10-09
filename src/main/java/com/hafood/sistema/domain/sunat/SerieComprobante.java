package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.estructura.Sede;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series_comprobante", uniqueConstraints = {
        @UniqueConstraint(name = "uk_serie_sede_tipo", columnNames = {"sede_id", "tipo"}),
        @UniqueConstraint(name = "uk_serie_codigo", columnNames = "serie")
})
@Getter
@Setter
@NoArgsConstructor
public class SerieComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoComprobante tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(name = "ultimo_correlativo", nullable = false)
    private long ultimoCorrelativo = 0;
}