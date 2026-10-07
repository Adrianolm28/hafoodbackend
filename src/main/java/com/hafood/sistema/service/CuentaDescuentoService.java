package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoDescuento;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaDescuento;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.repository.CuentaDescuentoRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CuentaDescuentoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaDescuentoRepository cuentaDescuentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaCalculoService calculoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public CuentaDTO aplicar(Long cuentaId, CuentaRequests.AplicarDescuento request, Usuario actor, Usuario autorizador) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);
        exigirAbierta(cuenta);
        calculoService.recalcular(cuenta);

        CuentaLinea linea = null;
        BigDecimal base;

        if (request.lineaId() != null) {
            linea = cuentaLineaRepository.findByIdAndCuentaId(request.lineaId(), cuentaId)
                    .filter(l -> l.getEstado() != EstadoLinea.ANULADA)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe en esta cuenta"));

            if (cuentaDescuentoRepository.existsByLineaIdAndActivoTrue(linea.getId())) {
                throw conflicto("El producto ya tiene un descuento o cortesía. Quítalo primero");
            }

            base = linea.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())).setScale(2, RoundingMode.HALF_UP);
        } else {
            if (cuentaDescuentoRepository.existsByCuentaIdAndLineaIsNullAndActivoTrue(cuentaId)) {
                throw conflicto("La cuenta ya tiene un descuento general. Quítalo primero");
            }

            base = cuenta.getTotal();
        }

        if (base.signum() <= 0) {
            throw invalida("No hay importe sobre el cual aplicar el descuento");
        }

        BigDecimal valor = null;
        Integer unidades = null;

        switch (request.tipo()) {
            case PORCENTAJE -> {
                valor = exigirValor(request.valor());
                if (valor.compareTo(CIEN) > 0) {
                    throw invalida("El porcentaje no puede superar 100");
                }
            }
            case MONTO -> {
                valor = exigirValor(request.valor());
                if (valor.compareTo(base) > 0) {
                    throw invalida("El descuento no puede superar el importe de S/ " + base.toPlainString());
                }
            }
            case CORTESIA -> {
                if (linea != null) {
                    unidades = request.cantidadCortesia() != null ? request.cantidadCortesia() : linea.getCantidad();
                    if (unidades < 1 || unidades > linea.getCantidad()) {
                        throw invalida("Las unidades de cortesía deben estar entre 1 y " + linea.getCantidad());
                    }
                }
            }
        }

        BigDecimal totalAntes = cuenta.getTotal();
        Usuario ejecutor = usuarioRepository.getReferenceById(actor.getId());
        Usuario quienAutoriza = usuarioRepository.getReferenceById(autorizador.getId());
        boolean autoAutorizado = autorizador.getId().equals(actor.getId());

        CuentaDescuento descuento = cuentaDescuentoRepository.save(CuentaDescuento.builder()
                .cuenta(cuenta)
                .linea(linea)
                .tipo(request.tipo())
                .valor(valor)
                .cantidadCortesia(unidades)
                .motivo(request.motivo().trim())
                .ejecutadoPor(ejecutor)
                .autorizadoPor(quienAutoriza)
                .autoAutorizado(autoAutorizado)
                .build());

        calculoService.recalcular(cuenta);

        if (descuento.getMontoDescuento().signum() <= 0) {
            throw invalida("El descuento no cambia el importe");
        }

        cuentaRepository.save(cuenta);

        AccionAuditoria accion = request.tipo() == TipoDescuento.CORTESIA
                ? AccionAuditoria.CORTESIA_APLICADA
                : AccionAuditoria.DESCUENTO_APLICADO;
        String objetivo = linea == null ? "cuenta" : linea.getCantidad() + "x " + linea.getNombre();

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, accion,
                "objetivo=" + objetivo + ";importe=" + base.toPlainString() + ";total=" + totalAntes.toPlainString(),
                "tipo=" + request.tipo() + (valor == null ? "" : ";valor=" + valor.toPlainString())
                        + (unidades == null ? "" : ";unidades=" + unidades)
                        + ";descuento=" + descuento.getMontoDescuento().toPlainString()
                        + ";total=" + cuenta.getTotal().toPlainString()
                        + ";ejecuta=" + actor.getUsername() + ";autoriza=" + autorizador.getUsername(),
                request.motivo().trim());

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public CuentaDTO quitar(Long cuentaId, Long descuentoId, CuentaRequests.QuitarDescuento request,
                            Usuario actor, Usuario autorizador) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);
        exigirAbierta(cuenta);
        calculoService.recalcular(cuenta);

        CuentaDescuento descuento = cuentaDescuentoRepository.findByIdAndCuentaId(descuentoId, cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El descuento no existe en esta cuenta"));

        if (!descuento.isActivo()) {
            throw conflicto("El descuento ya fue quitado");
        }

        BigDecimal totalAntes = cuenta.getTotal();
        String antes = "tipo=" + descuento.getTipo()
                + (descuento.getValor() == null ? "" : ";valor=" + descuento.getValor().toPlainString())
                + ";descuento=" + descuento.getMontoDescuento().toPlainString()
                + ";total=" + totalAntes.toPlainString();

        descuento.setActivo(false);
        descuento.setQuitadoEn(Instant.now());
        descuento.setQuitadoPor(usuarioRepository.getReferenceById(actor.getId()));
        descuento.setMotivoQuitado(request.motivo().trim());
        cuentaDescuentoRepository.save(descuento);

        calculoService.recalcular(cuenta);
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.DESCUENTO_QUITADO,
                antes, "total=" + cuenta.getTotal().toPlainString()
                        + ";ejecuta=" + actor.getUsername() + ";autoriza=" + autorizador.getUsername(),
                request.motivo().trim());

        return cuentaService.responder(cuenta);
    }

    private void exigirAbierta(Cuenta cuenta) {
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("Solo se pueden modificar descuentos en una cuenta abierta");
        }
    }

    private BigDecimal exigirValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw invalida("El valor del descuento es obligatorio y debe ser mayor a 0");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}