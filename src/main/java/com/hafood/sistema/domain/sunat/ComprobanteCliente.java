package com.hafood.sistema.domain.sunat;

import com.hafood.sistema.constant.TipoDocumentoCliente;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteCliente {

    @Enumerated(EnumType.STRING)
    @Column(name = "cliente_tipo_documento", nullable = false, length = 20)
    private TipoDocumentoCliente tipoDocumento;

    @Column(name = "cliente_numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "cliente_nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "cliente_direccion", length = 250)
    private String direccion;

    @Column(name = "cliente_correo", length = 120)
    private String correo;

    @Column(name = "cliente_telefono", length = 20)
    private String telefono;
}