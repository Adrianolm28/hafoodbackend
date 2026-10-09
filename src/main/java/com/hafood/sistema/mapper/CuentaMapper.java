package com.hafood.sistema.mapper;

import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaDescuento;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.pos.CuentaMesa;
import com.hafood.sistema.domain.pos.CuentaPago;
import com.hafood.sistema.domain.pos.CuentaPropina;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.service.CuentaCalculoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CuentaMapper {

    private CuentaMapper() {
    }

    public static CuentaDTO toDTO(Cuenta cuenta, List<CuentaMesa> mesas, CuentaCalculoService.Resultado resultado,
                                  List<CuentaPago> pagos, List<CuentaPropina> propinas, List<CuentaDTO.Hija> hijas) {
        BigDecimal pagado = pagos.stream()
                .map(CuentaPago::getAplicadoPen)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = cuenta.getTotalPrecuenta() != null ? cuenta.getTotalPrecuenta() : cuenta.getTotal();
        BigDecimal pendiente = base.subtract(pagado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal propinaTotal = propinas.stream()
                .map(CuentaPropina::getEquivalentePen)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new CuentaDTO(
                cuenta.getId(),
                cuenta.getSede().getId(),
                cuenta.getEstado(),
                cuenta.getCarta().getId(),
                cuenta.getCarta().getNombre(),
                cuenta.getMozo().getId(),
                cuenta.getMozo().getCodigo(),
                cuenta.getMozo().getNombre(),
                cuenta.getAbiertaPor().getUsername(),
                cuenta.getComensales(),
                cuenta.getNota(),
                cuenta.getAbiertaEn(),
                cuenta.getSubtotal(),
                cuenta.getDescuentoTotal(),
                cuenta.getTotal(),
                mesas.stream()
                        .map(f -> new CuentaDTO.MesaRef(
                                f.getMesa().getId(),
                                f.getMesa().getNombre(),
                                f.getMesa().getSeccion().getNombre()))
                        .toList(),
                resultado.lineas().stream().map(CuentaMapper::toLineaDTO).toList(),
                resultado.descuentos().stream().map(CuentaMapper::toDescuentoDTO).toList(),
                cuenta.getCajaSesion() == null ? null : cuenta.getCajaSesion().getId(),
                cuenta.getCajaSesion() == null ? null : cuenta.getCajaSesion().getCaja().getNombre(),
                pagado,
                pendiente,
                propinaTotal,
                cuenta.getCuentaPadre() == null ? null : cuenta.getCuentaPadre().getId(),
                hijas,
                pagos.stream().map(CuentaMapper::toPagoDTO).toList(),
                propinas.stream().map(CuentaMapper::toPropinaDTO).toList()
        );
    }

    private static CuentaDTO.Linea toLineaDTO(CuentaLinea linea) {
        return new CuentaDTO.Linea(
                linea.getId(),
                linea.getTipo(),
                linea.getProductoId(),
                linea.getNombre(),
                linea.getCantidad(),
                linea.getPrecioUnitario(),
                linea.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())).setScale(2, RoundingMode.HALF_UP),
                linea.getDescuentoLinea().add(linea.getDescuentoCuenta()),
                linea.getTotalLinea(),
                linea.getNota(),
                linea.getEstado(),
                linea.getAgregadaPor().getUsername(),
                linea.getAgregadaEn(),
                linea.getCuentaOrigen() == null ? null : linea.getCuentaOrigen().getId(),
                linea.getComanda() == null ? null : linea.getComanda().getId(),
                linea.getEnviadaEn(),
                linea.getPreparacionEn(),
                linea.getListaEn(),
                linea.getEntregadaEn()
        );
    }

    private static CuentaDTO.Descuento toDescuentoDTO(CuentaDescuento descuento) {
        return new CuentaDTO.Descuento(
                descuento.getId(),
                descuento.getLinea() == null ? null : descuento.getLinea().getId(),
                descuento.getTipo(),
                descuento.getValor(),
                descuento.getCantidadCortesia(),
                descuento.getMontoAntes(),
                descuento.getMontoDescuento(),
                descuento.getMontoDespues(),
                descuento.getMotivo(),
                descuento.getEjecutadoPor().getUsername(),
                descuento.getAutorizadoPor().getUsername(),
                descuento.isAutoAutorizado(),
                descuento.getCreadoEn()
        );
    }

    private static CuentaDTO.Pago toPagoDTO(CuentaPago pago) {
        return new CuentaDTO.Pago(
                pago.getId(),
                pago.getMetodo(),
                pago.getMarcaTarjeta(),
                pago.getMoneda(),
                pago.getRecibido(),
                pago.getTipoCambio(),
                pago.getAplicadoPen(),
                pago.getVueltoMonto(),
                pago.getVueltoMoneda(),
                pago.getVueltoPen(),
                pago.getReferencia(),
                pago.getRegistradoPor().getUsername(),
                pago.getCreadoEn()
        );
    }

    private static CuentaDTO.Propina toPropinaDTO(CuentaPropina propina) {
        return new CuentaDTO.Propina(
                propina.getId(),
                propina.getMetodo(),
                propina.getMarcaTarjeta(),
                propina.getMoneda(),
                propina.getMonto(),
                propina.getEquivalentePen(),
                propina.getPorcentaje(),
                propina.getRegistradoPor().getUsername(),
                propina.getCreadoEn()
        );
    }
}