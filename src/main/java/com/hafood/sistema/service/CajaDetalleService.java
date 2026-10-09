package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.domain.caja.CajaArqueo;
import com.hafood.sistema.domain.caja.CajaArqueoLinea;
import com.hafood.sistema.domain.caja.CajaMovimiento;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaSesionDTO;
import com.hafood.sistema.mapper.CajaMapper;
import com.hafood.sistema.repository.CajaArqueoLineaRepository;
import com.hafood.sistema.repository.CajaArqueoRepository;
import com.hafood.sistema.repository.CajaMovimientoRepository;
import com.hafood.sistema.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CajaDetalleService {

    public static final List<EstadoCuenta> CUENTAS_PENDIENTES =
            List.of(EstadoCuenta.ABIERTA, EstadoCuenta.PRECUENTA, EstadoCuenta.PAGO_PARCIAL, EstadoCuenta.PAGADA);

    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final CajaArqueoRepository cajaArqueoRepository;
    private final CajaArqueoLineaRepository cajaArqueoLineaRepository;
    private final CuentaRepository cuentaRepository;
    private final CajaCalculoService calculoService;
    private final AutorizacionService autorizacionService;

    @Transactional(readOnly = true)
    public CajaSesionDTO detalle(CajaSesion sesion, Usuario actor) {
        boolean supervisor = autorizacionService.esSupervisor(actor);
        Long id = sesion.getId();

        List<CajaMovimiento> movimientos = cajaMovimientoRepository.findBySesionId(id);
        Set<Long> anulados = movimientos.stream()
                .map(CajaMovimiento::getAnulaMovimientoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CajaArqueo> arqueos = cajaArqueoRepository.findBySesionId(id);
        Map<Long, List<CajaArqueoLinea>> lineasPorArqueo = arqueos.isEmpty()
                ? Map.of()
                : cajaArqueoLineaRepository
                .findByArqueoIdInOrderByIdAsc(arqueos.stream().map(CajaArqueo::getId).toList()).stream()
                .collect(Collectors.groupingBy(l -> l.getArqueo().getId()));

        List<CajaSesionDTO.Saldo> saldos = supervisor
                ? calculoService.saldos(id).stream()
                .map(s -> new CajaSesionDTO.Saldo(s.clave().metodo(), s.clave().marca(), s.clave().moneda(),
                        s.apertura(), s.cobros(), s.propinas(), s.ingresos(), s.egresos(), s.devoluciones(),
                        s.vueltos(), s.esperado()))
                .toList()
                : null;

        return new CajaSesionDTO(
                CajaMapper.toResumen(sesion, supervisor),
                saldos,
                movimientos.stream().map(m -> aMovimiento(m, anulados)).toList(),
                arqueos.stream().map(a -> aArqueo(a, lineasPorArqueo.getOrDefault(a.getId(), List.of()), supervisor)).toList(),
                cuentaRepository.countByCajaSesionIdAndEstadoIn(id, CUENTAS_PENDIENTES)
        );
    }

    private CajaSesionDTO.Movimiento aMovimiento(CajaMovimiento m, Set<Long> anulados) {
        return new CajaSesionDTO.Movimiento(m.getId(), m.getTipo(), m.getMetodo(), m.getMarcaTarjeta(), m.getMoneda(),
                m.getMonto(), m.getTipoCambio(), m.getEquivalentePen(), m.getCategoria(), m.getMotivo(),
                m.getBeneficiario(), m.getRegistradoPor().getUsername(), m.getCreadoEn(), m.getCuentaId(),
                m.getAnulaMovimientoId(), anulados.contains(m.getId()), m.getReferencia());
    }

    private CajaSesionDTO.Arqueo aArqueo(CajaArqueo a, List<CajaArqueoLinea> lineas, boolean supervisor) {
        return new CajaSesionDTO.Arqueo(a.getId(), a.getTipo(), a.getRealizadoPor().getUsername(), a.getRealizadoEn(),
                a.getObservacion(), a.getMotivoDiferencia(), a.isHayDiferencia(),
                lineas.stream().map(l -> new CajaSesionDTO.ArqueoLinea(l.getMetodo(), l.getMarcaTarjeta(), l.getMoneda(),
                        l.getContado(), supervisor ? l.getEsperado() : null, supervisor ? l.getDiferencia() : null,
                        denominaciones(l.getDetalle()))).toList());
    }

    private List<CajaSesionDTO.Denominacion> denominaciones(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return List.of();
        }

        return Arrays.stream(detalle.split(","))
                .map(par -> par.split(":"))
                .map(par -> new CajaSesionDTO.Denominacion(new BigDecimal(par[0]), Integer.parseInt(par[1])))
                .toList();
    }
}