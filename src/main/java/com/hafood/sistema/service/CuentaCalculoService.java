package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaDescuento;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.repository.CuentaDescuentoRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaCalculoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaDescuentoRepository cuentaDescuentoRepository;

    public record Resultado(List<CuentaLinea> lineas, List<CuentaDescuento> descuentos) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Resultado recalcular(Cuenta cuenta) {
        List<CuentaLinea> todas = cuentaLineaRepository.findByCuentaIdConUsuario(cuenta.getId());
        List<CuentaDescuento> descuentos = cuentaDescuentoRepository.findActivosByCuentaId(cuenta.getId());

        todas.stream().filter(l -> l.getEstado() == EstadoLinea.ANULADA).forEach(l -> {
            l.setDescuentoLinea(BigDecimal.ZERO);
            l.setDescuentoCuenta(BigDecimal.ZERO);
            l.setTotalLinea(BigDecimal.ZERO);
        });

        List<CuentaLinea> vivas = todas.stream().filter(l -> l.getEstado() != EstadoLinea.ANULADA).toList();

        Map<Long, CuentaDescuento> porLinea = descuentos.stream()
                .filter(d -> d.getLinea() != null)
                .collect(Collectors.toMap(d -> d.getLinea().getId(), Function.identity(), (a, b) -> b));
        CuentaDescuento general = descuentos.stream().filter(d -> d.getLinea() == null).findFirst().orElse(null);

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> netos = new LinkedHashMap<>();

        for (CuentaLinea linea : vivas) {
            BigDecimal bruto = bruto(linea);
            BigDecimal descuentoLinea = BigDecimal.ZERO;
            CuentaDescuento propio = porLinea.get(linea.getId());

            if (propio != null) {
                descuentoLinea = descuentoDeLinea(propio, linea, bruto);
                propio.setMontoAntes(bruto);
                propio.setMontoDescuento(descuentoLinea);
                propio.setMontoDespues(bruto.subtract(descuentoLinea));
            }

            linea.setDescuentoLinea(descuentoLinea);
            linea.setDescuentoCuenta(BigDecimal.ZERO);
            netos.put(linea.getId(), bruto.subtract(descuentoLinea));
            subtotal = subtotal.add(bruto);
        }

        BigDecimal sumaNeto = netos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (general != null) {
            BigDecimal pedido = descuentoGeneral(general, sumaNeto);
            BigDecimal asignado = prorratear(vivas, netos, sumaNeto, pedido);
            general.setMontoAntes(sumaNeto);
            general.setMontoDescuento(asignado);
            general.setMontoDespues(sumaNeto.subtract(asignado));
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;

        for (CuentaLinea linea : vivas) {
            BigDecimal totalLinea = netos.get(linea.getId()).subtract(linea.getDescuentoCuenta());
            linea.setTotalLinea(totalLinea);
            total = total.add(totalLinea);
            descuentoTotal = descuentoTotal.add(linea.getDescuentoLinea()).add(linea.getDescuentoCuenta());
        }

        cuenta.setSubtotal(subtotal);
        cuenta.setDescuentoTotal(descuentoTotal);
        cuenta.setTotal(total);

        return new Resultado(vivas, descuentos);
    }

    private BigDecimal bruto(CuentaLinea linea) {
        return linea.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal descuentoDeLinea(CuentaDescuento descuento, CuentaLinea linea, BigDecimal bruto) {
        return switch (descuento.getTipo()) {
            case PORCENTAJE -> bruto.multiply(descuento.getValor()).divide(CIEN, 2, RoundingMode.HALF_UP);
            case MONTO -> descuento.getValor().min(bruto);
            case CORTESIA -> {
                int pedidas = descuento.getCantidadCortesia() == null ? linea.getCantidad() : descuento.getCantidadCortesia();
                int unidades = Math.min(pedidas, linea.getCantidad());
                yield linea.getPrecioUnitario().multiply(BigDecimal.valueOf(unidades)).setScale(2, RoundingMode.HALF_UP);
            }
        };
    }

    private BigDecimal descuentoGeneral(CuentaDescuento descuento, BigDecimal sumaNeto) {
        return switch (descuento.getTipo()) {
            case PORCENTAJE -> sumaNeto.multiply(descuento.getValor()).divide(CIEN, 2, RoundingMode.HALF_UP);
            case MONTO -> descuento.getValor().min(sumaNeto);
            case CORTESIA -> sumaNeto;
        };
    }

    private BigDecimal prorratear(List<CuentaLinea> vivas, Map<Long, BigDecimal> netos,
                                  BigDecimal sumaNeto, BigDecimal descuento) {
        if (descuento.signum() == 0 || sumaNeto.signum() == 0) {
            return BigDecimal.ZERO;
        }

        List<CuentaLinea> candidatas = vivas.stream().filter(l -> netos.get(l.getId()).signum() > 0).toList();
        BigDecimal asignado = BigDecimal.ZERO;

        for (int i = 0; i < candidatas.size(); i++) {
            CuentaLinea linea = candidatas.get(i);
            BigDecimal neto = netos.get(linea.getId());
            BigDecimal parte = i == candidatas.size() - 1
                    ? descuento.subtract(asignado)
                    : descuento.multiply(neto).divide(sumaNeto, 2, RoundingMode.HALF_UP);
            parte = parte.min(neto).max(BigDecimal.ZERO);
            linea.setDescuentoCuenta(parte);
            asignado = asignado.add(parte);
        }

        return asignado;
    }
}