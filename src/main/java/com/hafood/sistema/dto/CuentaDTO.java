package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.constant.TipoDescuento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CuentaDTO(
        Long id,
        Long sedeId,
        EstadoCuenta estado,
        Long cartaId,
        String cartaNombre,
        Long mozoId,
        Integer mozoCodigo,
        String mozoNombre,
        String abiertaPorNombre,
        Integer comensales,
        String nota,
        Instant abiertaEn,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal total,
        List<MesaRef> mesas,
        List<Linea> lineas,
        List<Descuento> descuentos
) {

    public record MesaRef(Long id, String nombre, String seccionNombre) {
    }

    public record Linea(
            Long id,
            TipoCategoria tipo,
            Long productoId,
            String nombre,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal totalLinea,
            String nota,
            EstadoLinea estado,
            String agregadaPorNombre,
            Instant agregadaEn,
            Long cuentaOrigenId,
            Long comandaId,
            Instant enviadaEn,
            Instant preparacionEn,
            Instant listaEn,
            Instant entregadaEn
    ) {
    }

    public record Descuento(
            Long id,
            Long lineaId,
            TipoDescuento tipo,
            BigDecimal valor,
            Integer cantidadCortesia,
            BigDecimal montoAntes,
            BigDecimal montoDescuento,
            BigDecimal montoDespues,
            String motivo,
            String ejecutadoPorNombre,
            String autorizadoPorNombre,
            boolean autoAutorizado,
            Instant creadoEn
    ) {
    }
}