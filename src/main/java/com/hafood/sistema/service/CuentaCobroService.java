package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.domain.caja.CajaMovimiento;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.pos.CuentaPago;
import com.hafood.sistema.domain.pos.CuentaPropina;
import com.hafood.sistema.domain.pos.OperacionProcesada;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.OpcionVueltoDTO;
import com.hafood.sistema.dto.request.CobroRequests;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaPagoRepository;
import com.hafood.sistema.repository.CuentaPropinaRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.OperacionProcesadaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuentaCobroService {

    private static final String OPERACION_PAGAR = "PAGAR";
    private static final String OPERACION_PROPINA = "PROPINA";

    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaPagoRepository cuentaPagoRepository;
    private final CuentaPropinaRepository cuentaPropinaRepository;
    private final OperacionProcesadaRepository operacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaAccesoService cajaAccesoService;
    private final CajaMovimientoService cajaMovimientoService;
    private final CajaCalculoService cajaCalculoService;
    private final CajaSesionService cajaSesionService;
    private final CuentaCalculoService calculoService;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;

    private record Bloqueo(CajaSesion sesion, Cuenta cuenta) {
    }

    @Transactional
    public CuentaDTO precuenta(Long cuentaId, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("Solo una cuenta abierta pasa a precuenta");
        }

        List<CuentaLinea> vivas = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId).stream()
                .filter(l -> l.getEstado() != EstadoLinea.ANULADA)
                .toList();

        if (vivas.isEmpty()) {
            throw conflicto("La cuenta no tiene productos");
        }
        if (vivas.stream().anyMatch(l -> l.getEstado() == EstadoLinea.BORRADOR)) {
            throw conflicto("Hay productos sin enviar. Envíalos o quítalos antes de pedir la precuenta");
        }

        calculoService.recalcular(cuenta);
        cuenta.setTotalPrecuenta(cuenta.getTotal());
        cuenta.setPrecuentaEn(Instant.now());
        cuenta.setEstado(cuenta.getTotal().signum() == 0 ? EstadoCuenta.PAGADA : EstadoCuenta.PRECUENTA);
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.CUENTA_PRECUENTA,
                "estado=" + EstadoCuenta.ABIERTA,
                "estado=" + cuenta.getEstado() + ";total=" + cuenta.getTotal().toPlainString(), null);

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public CuentaDTO reabrir(Long cuentaId, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (cuenta.getCuentaPadre() != null) {
            throw conflicto("Una cuenta separada no se reabre. Reúnela primero con la principal");
        }
        if (cuenta.getEstado() != EstadoCuenta.PRECUENTA) {
            throw conflicto("Solo se puede reabrir una cuenta en precuenta");
        }
        if (cuentaPagoRepository.existsByCuentaId(cuentaId)) {
            throw conflicto("La cuenta ya tiene pagos y no se puede reabrir");
        }
        if (cuentaRepository.existsByCuentaPadreIdAndEstadoIn(cuentaId, CajaDetalleService.CUENTAS_PENDIENTES)) {
            throw conflicto("La cuenta tiene cuentas separadas. Reúnelas antes de reabrir");
        }

        BigDecimal congelado = cuenta.getTotalPrecuenta();
        cuenta.setEstado(EstadoCuenta.ABIERTA);
        cuenta.setTotalPrecuenta(null);
        cuenta.setPrecuentaEn(null);
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.CUENTA_REABIERTA,
                "estado=" + EstadoCuenta.PRECUENTA + ";total=" + (congelado == null ? "-" : congelado.toPlainString()),
                "estado=" + EstadoCuenta.ABIERTA, null);

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public CuentaDTO pagar(Long cuentaId, CobroRequests.Pagar request, Usuario actor) {
        Bloqueo bloqueo = bloquearConCaja(cuentaId, actor);
        Cuenta cuenta = bloqueo.cuenta();
        CajaSesion sesion = bloqueo.sesion();

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            return cuentaService.responder(cuenta);
        }

        cajaAccesoService.exigirAbierta(sesion);
        exigirCobrable(cuenta);

        BigDecimal base = exigirTotalCongelado(cuenta);
        BigDecimal pagadoAntes = cuentaPagoRepository.totalAplicado(cuentaId);
        BigDecimal pendiente = base.subtract(pagadoAntes).setScale(2, RoundingMode.HALF_UP);

        if (pendiente.signum() <= 0) {
            throw conflicto("La cuenta ya está pagada");
        }

        MetodoPago metodo = request.metodo();
        Moneda moneda = request.moneda();
        boolean efectivo = metodo == MetodoPago.EFECTIVO;
        MarcaTarjeta marca = validarMarca(metodo, request.marca());
        String referencia = validarReferencia(efectivo, request.referencia());

        if (!efectivo && moneda != Moneda.PEN) {
            throw invalida("Solo el efectivo puede recibirse en dólares");
        }

        BigDecimal recibido = request.recibido().setScale(2, RoundingMode.HALF_UP);
        BigDecimal recibidoPen = aSolesExacto(moneda, recibido);
        BigDecimal aplicado;

        if (efectivo) {
            aplicado = request.aplicar() != null
                    ? request.aplicar().setScale(2, RoundingMode.HALF_UP)
                    : recibidoPen.min(pendiente);

            if (aplicado.compareTo(pendiente) > 0) {
                throw invalida("El monto a cobrar supera el saldo pendiente de S/ " + pendiente.toPlainString());
            }
            if (aplicado.compareTo(recibidoPen) > 0) {
                throw invalida("El efectivo recibido no alcanza para el monto a cobrar");
            }
        } else {
            aplicado = recibidoPen;

            if (request.aplicar() != null && request.aplicar().compareTo(recibidoPen) != 0) {
                throw invalida("En este método el monto cobrado debe ser igual al recibido");
            }
            if (aplicado.compareTo(pendiente) > 0) {
                throw invalida("El monto supera el saldo pendiente de S/ " + pendiente.toPlainString());
            }
        }

        BigDecimal vueltoPen = recibidoPen.subtract(aplicado).setScale(2, RoundingMode.HALF_UP);
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String clave = request.claveIdempotencia();

        CajaMovimiento cobro = cajaMovimientoService.crear(sesion, TipoMovimientoCaja.COBRO, metodo, marca, moneda,
                recibido, usuario, null, "Cobro de cuenta #" + cuentaId, null, cuentaId, referencia, null,
                clave + "-C");

        if (cobro.getEquivalentePen().compareTo(recibidoPen) != 0) {
            throw conflicto("El importe del cobro no cuadra con su equivalente en soles");
        }

        Moneda vueltoMoneda = null;
        BigDecimal vueltoMonto = null;
        CajaMovimiento vuelto = null;

        if (vueltoPen.signum() > 0) {
            vueltoMoneda = request.vueltoMoneda() == null ? Moneda.PEN : request.vueltoMoneda();
            vueltoMonto = vueltoMoneda == Moneda.PEN ? vueltoPen : vueltoEnDolares(vueltoPen);
            exigirEfectivoCaja(sesion.getId(), vueltoMoneda, vueltoMonto);

            vuelto = cajaMovimientoService.crear(sesion, TipoMovimientoCaja.VUELTO, MetodoPago.EFECTIVO, null,
                    vueltoMoneda, vueltoMonto, usuario, null, "Vuelto de cuenta #" + cuentaId, null, cuentaId,
                    referencia, null, clave + "-V");

            if (vuelto.getEquivalentePen().compareTo(vueltoPen) != 0) {
                throw conflicto("El importe del vuelto no cuadra con su equivalente en soles");
            }
        }

        cuentaPagoRepository.save(CuentaPago.builder()
                .cuenta(cuenta)
                .cajaSesion(sesion)
                .metodo(metodo)
                .marcaTarjeta(marca)
                .moneda(moneda)
                .recibido(recibido)
                .tipoCambio(moneda.tipoCambio())
                .recibidoPen(recibidoPen)
                .aplicadoPen(aplicado)
                .vueltoMonto(vueltoMonto)
                .vueltoMoneda(vueltoMoneda)
                .vueltoPen(vueltoPen.signum() > 0 ? vueltoPen : null)
                .referencia(referencia)
                .registradoPor(usuario)
                .movimientoId(cobro.getId())
                .movimientoVueltoId(vuelto == null ? null : vuelto.getId())
                .build());

        BigDecimal pendienteDespues = pendiente.subtract(aplicado).setScale(2, RoundingMode.HALF_UP);
        EstadoCuenta anterior = cuenta.getEstado();
        cuenta.setEstado(pendienteDespues.signum() == 0 ? EstadoCuenta.PAGADA : EstadoCuenta.PAGO_PARCIAL);
        cuentaRepository.save(cuenta);

        operacionRepository.save(OperacionProcesada.builder()
                .clave(clave)
                .tipo(OPERACION_PAGAR)
                .cuentaId(cuentaId)
                .build());

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.PAGO_REGISTRADO,
                "estado=" + anterior + ";pendiente=" + pendiente.toPlainString(),
                "metodo=" + metodo + (marca == null ? "" : " " + marca)
                        + ";recibido=" + moneda + " " + recibido.toPlainString()
                        + ";aplicado=" + aplicado.toPlainString()
                        + ";vuelto=" + (vuelto == null ? "0.00" : vueltoMoneda + " " + vueltoMonto.toPlainString())
                        + ";pendiente=" + pendienteDespues.toPlainString()
                        + ";estado=" + cuenta.getEstado()
                        + ";caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesion.getId(),
                null);

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public CuentaDTO propina(Long cuentaId, CobroRequests.Propina request, Usuario actor) {
        Bloqueo bloqueo = bloquearConCaja(cuentaId, actor);
        Cuenta cuenta = bloqueo.cuenta();
        CajaSesion sesion = bloqueo.sesion();

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            return cuentaService.responder(cuenta);
        }

        cajaAccesoService.exigirAbierta(sesion);

        if (cuenta.getEstado() != EstadoCuenta.PAGO_PARCIAL && cuenta.getEstado() != EstadoCuenta.PAGADA) {
            throw conflicto("La propina se registra después de iniciar el cobro");
        }

        MetodoPago metodo = request.metodo();
        Moneda moneda = request.moneda();
        boolean efectivo = metodo == MetodoPago.EFECTIVO;
        MarcaTarjeta marca = validarMarca(metodo, request.marca());
        String referencia = validarReferencia(efectivo, request.referencia());

        if (!efectivo && moneda != Moneda.PEN) {
            throw invalida("Solo el efectivo puede recibirse en dólares");
        }

        BigDecimal monto = request.monto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal equivalente = aSolesExacto(moneda, monto);
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());

        CajaMovimiento movimiento = cajaMovimientoService.crear(sesion, TipoMovimientoCaja.PROPINA, metodo, marca,
                moneda, monto, usuario, null, "Propina de cuenta #" + cuentaId, null, cuentaId, referencia, null,
                request.claveIdempotencia() + "-P");

        cuentaPropinaRepository.save(CuentaPropina.builder()
                .cuenta(cuenta)
                .cajaSesion(sesion)
                .metodo(metodo)
                .marcaTarjeta(marca)
                .moneda(moneda)
                .monto(monto)
                .tipoCambio(moneda.tipoCambio())
                .equivalentePen(equivalente)
                .porcentaje(request.porcentaje())
                .registradoPor(usuario)
                .movimientoId(movimiento.getId())
                .build());

        operacionRepository.save(OperacionProcesada.builder()
                .clave(request.claveIdempotencia())
                .tipo(OPERACION_PROPINA)
                .cuentaId(cuentaId)
                .build());

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.PROPINA_REGISTRADA,
                null,
                "metodo=" + metodo + (marca == null ? "" : " " + marca) + ";monto=" + moneda + " "
                        + monto.toPlainString() + ";equivalentePen=" + equivalente.toPlainString()
                        + ";caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesion.getId(),
                null);

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public CuentaDTO cerrar(Long cuentaId, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (cuenta.getEstado() == EstadoCuenta.CERRADA) {
            return cuentaService.responder(cuenta);
        }
        if (cuenta.getEstado() != EstadoCuenta.PAGADA) {
            throw conflicto("Solo se puede cerrar una cuenta pagada");
        }

        BigDecimal base = exigirTotalCongelado(cuenta);

        if (cuentaPagoRepository.totalAplicado(cuentaId).compareTo(base) != 0) {
            throw conflicto("La cuenta aún tiene saldo pendiente");
        }
        if (cuentaRepository.existsByCuentaPadreIdAndEstadoIn(cuentaId, CajaDetalleService.CUENTAS_PENDIENTES)) {
            throw conflicto("Hay cuentas separadas sin cerrar. Ciérralas primero");
        }

        Instant ahora = Instant.now();

        if (cuenta.getCuentaPadre() == null) {
            cuentaService.liberarMesas(cuenta, ahora);
        }

        cuenta.setEstado(EstadoCuenta.CERRADA);
        cuenta.setCerradaEn(ahora);
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.CUENTA_CERRADA,
                "estado=" + EstadoCuenta.PAGADA,
                "estado=" + EstadoCuenta.CERRADA + ";total=" + base.toPlainString(), null);

        return cuentaService.responder(cuenta);
    }

    @Transactional(readOnly = true)
    public OpcionVueltoDTO opcionesVuelto(Long cuentaId, BigDecimal vueltoPen, BigDecimal recibidoUsd, Usuario actor) {
        Long sesionId = cuentaRepository.findCajaSesionIdById(cuentaId)
                .orElseThrow(() -> conflicto("La cuenta no existe o no tiene una caja asignada"));
        sedeAccesoService.exigirAcceso(actor, cajaSesionService.sedeIdDe(sesionId));

        if (vueltoPen == null || vueltoPen.signum() <= 0) {
            return new OpcionVueltoDTO(false, null, "No hay vuelto que dar");
        }

        BigDecimal exacto = vueltoPen.setScale(2, RoundingMode.HALF_UP);
        BigDecimal dolares = exacto.divide(Moneda.USD.tipoCambio(), 2, RoundingMode.HALF_UP);

        if (Moneda.USD.aSoles(dolares).compareTo(exacto) != 0) {
            return new OpcionVueltoDTO(false, null, "El vuelto no se puede dar en dólares exactos. Entrégalo en soles");
        }

        BigDecimal disponible = cajaCalculoService.saldo(sesionId, MetodoPago.EFECTIVO, null, Moneda.USD)
                .add(recibidoUsd == null ? BigDecimal.ZERO : recibidoUsd);

        if (disponible.compareTo(dolares) < 0) {
            return new OpcionVueltoDTO(false, dolares, "La caja no tiene dólares suficientes para este vuelto");
        }

        return new OpcionVueltoDTO(true, dolares, null);
    }

    private Bloqueo bloquearConCaja(Long cuentaId, Usuario actor) {
        Long sesionId = cuentaRepository.findCajaSesionIdById(cuentaId)
                .orElseThrow(() -> conflicto("La cuenta no existe o no tiene una caja asignada"));
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (cuenta.getCajaSesion() == null || !cuenta.getCajaSesion().getId().equals(sesionId)) {
            throw conflicto("La cuenta cambió de caja. Actualiza la pantalla e intenta de nuevo");
        }

        return new Bloqueo(sesion, cuenta);
    }

    private void exigirCobrable(Cuenta cuenta) {
        EstadoCuenta estado = cuenta.getEstado();

        if (estado == EstadoCuenta.ABIERTA) {
            throw conflicto("Pide la precuenta antes de cobrar");
        }
        if (estado != EstadoCuenta.PRECUENTA && estado != EstadoCuenta.PAGO_PARCIAL) {
            throw conflicto("La cuenta ya no admite cobros");
        }
    }

    private BigDecimal exigirTotalCongelado(Cuenta cuenta) {
        calculoService.recalcular(cuenta);
        BigDecimal base = cuenta.getTotalPrecuenta();

        if (base == null || cuenta.getTotal().compareTo(base) != 0) {
            throw conflicto("El total de la cuenta cambió. Reabre la cuenta y pide la precuenta otra vez");
        }

        return base.setScale(2, RoundingMode.HALF_UP);
    }

    private MarcaTarjeta validarMarca(MetodoPago metodo, MarcaTarjeta marca) {
        if (metodo != MetodoPago.TARJETA) {
            return null;
        }
        if (marca == null) {
            throw invalida("Elige la marca de la tarjeta");
        }
        return marca;
    }

    private String validarReferencia(boolean efectivo, String referencia) {
        String limpia = referencia == null || referencia.isBlank() ? null : referencia.trim();

        if (!efectivo && limpia == null) {
            throw invalida("Indica el número de operación o voucher");
        }

        return efectivo ? null : limpia;
    }

    private BigDecimal aSolesExacto(Moneda moneda, BigDecimal monto) {
        BigDecimal soles = monto.multiply(moneda.tipoCambio());

        if (soles.stripTrailingZeros().scale() > 2) {
            throw invalida("Ese monto en dólares genera fracciones de céntimo al convertirlo a soles. Ajusta el monto");
        }

        return soles.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal vueltoEnDolares(BigDecimal vueltoPen) {
        BigDecimal dolares = vueltoPen.divide(Moneda.USD.tipoCambio(), 2, RoundingMode.HALF_UP);

        if (Moneda.USD.aSoles(dolares).compareTo(vueltoPen) != 0) {
            throw conflicto("El vuelto no se puede dar en dólares exactos. Entrégalo en soles");
        }

        return dolares;
    }

    private void exigirEfectivoCaja(Long sesionId, Moneda moneda, BigDecimal monto) {
        BigDecimal disponible = cajaCalculoService.saldo(sesionId, MetodoPago.EFECTIVO, null, moneda);

        if (disponible.compareTo(monto) < 0) {
            throw conflicto("La caja no tiene suficiente efectivo en " + (moneda == Moneda.USD ? "dólares" : "soles")
                    + " para dar el vuelto. Registra un ingreso de cambio o da el vuelto en otra moneda");
        }
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}