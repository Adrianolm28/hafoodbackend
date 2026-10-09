package com.hafood.sistema.domain.sunat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "comprobante_archivos",
        uniqueConstraints = @UniqueConstraint(name = "uk_comprobante_archivo_comprobante", columnNames = "comprobante_id"))
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(name = "nombre_archivo", length = 60)
    private String nombreArchivo;

    @Column(name = "xml", columnDefinition = "bytea")
    private byte[] xml;

    @Column(nullable = false)
    private boolean firmado = false;

    @Column(name = "xml_generado_en")
    private Instant xmlGeneradoEn;

    @Column(name = "error_generacion", length = 300)
    private String errorGeneracion;

    @Column(name = "hash_firma", length = 100)
    private String hashFirma;

    @Column(name = "firmado_en")
    private Instant firmadoEn;

    @Version
    private Long version;
}