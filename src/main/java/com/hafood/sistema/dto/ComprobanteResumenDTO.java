package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ComprobanteResumenDTO(
        Long id,
        Long cuentaId,
        Long sedeId,
        TipoComprobante tipo,
        String numero,
        LocalDate fechaEmision,
        Instant emitidoEn,
        EstadoSunat estadoSunat,
        String clienteNombre,
        TipoDocumentoCliente clienteTipoDocumento,
        String clienteNumeroDocumento,
        BigDecimal total) {
}