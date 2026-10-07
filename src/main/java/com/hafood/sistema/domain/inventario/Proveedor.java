package com.hafood.sistema.domain.inventario;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 11)
    private String ruc;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String contacto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}