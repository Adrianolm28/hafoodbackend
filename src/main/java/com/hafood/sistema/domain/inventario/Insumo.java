package com.hafood.sistema.domain.inventario;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.constant.UnidadBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "insumos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60)
    private String nombre;

    @Column(name = "unidad_medida")
    private String unidadMedida;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_base", nullable = false, length = 10)
    private UnidadBase unidadBase;

    @Column(name = "presentacion_nombre", length = 30)
    private String presentacionNombre;

    @Column(name = "presentacion_cantidad", precision = 12, scale = 3)
    private BigDecimal presentacionCantidad;

    @Column(name = "costo_unitario", precision = 14, scale = 6)
    private BigDecimal costoUnitario;

    @Column(name = "control_estricto", nullable = false)
    @Builder.Default
    private boolean controlEstricto = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AreaInsumo area;
}
