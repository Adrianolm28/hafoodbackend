package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ComprobanteDTO(
        Long id,
        Long cuentaId,
        Long sedeId,
        TipoComprobante tipo,
        String serie,
        long correlativo,
        String numero,
        LocalDate fechaEmision,
        Instant emitidoEn,
        String emitidoPor,
        EstadoSunat estadoSunat,
        String rucEmisor,
        String razonSocialEmisor,
        RegimenTributario regimen,
        BigDecimal igvPorcentaje,
        BigDecimal ipmPorcentaje,
        Cliente cliente,
        BigDecimal opGravada,
        BigDecimal igv,
        BigDecimal ipm,
        BigDecimal total,
        List<Linea> lineas) {

    public record Cliente(
            TipoDocumentoCliente tipoDocumento,
            String numeroDocumento,
            String nombre,
            String direccion,
            String correo,
            String telefono) {
    }

    public record Linea(
            int item,
            String descripcion,
            String unidadMedida,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal total,
            BigDecimal baseImponible,
            BigDecimal igv,
            BigDecimal ipm) {
    }
}