package com.hafood.sistema.dto;

import com.hafood.sistema.constant.CategoriaCaja;
import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoArqueo;
import com.hafood.sistema.constant.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CajaSesionDTO(
        CajaDTO.SesionResumen sesion,
        List<Saldo> saldos,
        List<Movimiento> movimientos,
        List<Arqueo> arqueos,
        long cuentasPendientes
) {

    public record Saldo(
            MetodoPago metodo,
            MarcaTarjeta marca,
            Moneda moneda,
            BigDecimal apertura,
            BigDecimal cobros,
            BigDecimal propinas,
            BigDecimal ingresos,
            BigDecimal egresos,
            BigDecimal devoluciones,
            BigDecimal vueltos,
            BigDecimal esperado
    ) {
    }

    public record Movimiento(
            Long id,
            TipoMovimientoCaja tipo,
            MetodoPago metodo,
            MarcaTarjeta marca,
            Moneda moneda,
            BigDecimal monto,
            BigDecimal tipoCambio,
            BigDecimal equivalentePen,
            CategoriaCaja categoria,
            String motivo,
            String beneficiario,
            String registradoPorNombre,
            Instant creadoEn,
            Long cuentaId,
            Long anulaMovimientoId,
            boolean anulado,
            String referencia
    ) {
    }

    public record Denominacion(BigDecimal valor, int cantidad) {
    }

    public record ArqueoLinea(
            MetodoPago metodo,
            MarcaTarjeta marca,
            Moneda moneda,
            BigDecimal contado,
            BigDecimal esperado,
            BigDecimal diferencia,
            List<Denominacion> denominaciones
    ) {
    }

    public record Arqueo(
            Long id,
            TipoArqueo tipo,
            String realizadoPorNombre,
            Instant realizadoEn,
            String observacion,
            String motivoDiferencia,
            boolean hayDiferencia,
            List<ArqueoLinea> lineas
    ) {
    }
}