package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.domain.estructura.Sede;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sedes_sunat", uniqueConstraints = @UniqueConstraint(name = "uk_sede_sunat_sede", columnNames = "sede_id"))
@Getter
@Setter
@NoArgsConstructor
public class SedeSunat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "codigo_establecimiento", nullable = false, length = 4)
    private String codigoEstablecimiento;

    @Column(name = "direccion_fiscal", nullable = false, length = 250)
    private String direccionFiscal;

    @Column(nullable = false, length = 6)
    private String ubigeo;
}