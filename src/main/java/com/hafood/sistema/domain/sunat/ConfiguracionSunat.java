package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "configuracion_sunat", uniqueConstraints = @UniqueConstraint(name = "uk_config_sunat_unica", columnNames = "clave_unica"))
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSunat {

    public static final String CLAVE_UNICA = "EMPRESA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_unica", nullable = false, updatable = false, length = 10)
    private String claveUnica = CLAVE_UNICA;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    @Column(name = "usuario_sol", length = 30)
    private String usuarioSol;

    @Column(name = "clave_sol_cifrada", length = 600)
    private String claveSolCifrada;

    @Column(name = "nombre_certificado", length = 200)
    private String nombreCertificado;

    @Column(name = "certificado_cifrado", columnDefinition = "bytea")
    private byte[] certificadoCifrado;

    @Column(name = "certificado_password_cifrada", length = 600)
    private String certificadoPasswordCifrada;

    @Column(name = "certificado_vence")
    private LocalDate certificadoVence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AmbienteSunat ambiente = AmbienteSunat.BETA;

    @Column(nullable = false)
    private boolean activa = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private RegimenTributario regimen;

    @Column(name = "umbral_boleta_sin_documento", precision = 12, scale = 2)
    private BigDecimal umbralBoletaSinDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private Usuario actualizadoPor;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Version
    private Long version;
}