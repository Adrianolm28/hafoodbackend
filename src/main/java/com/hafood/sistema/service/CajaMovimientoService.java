package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.CategoriaCaja;
import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.domain.caja.CajaMovimiento;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaSesionDTO;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.repository.CajaMovimientoRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CajaMovimientoService {

    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaAccesoService cajaAccesoService;
    private final CajaDetalleService cajaDetalleService;
    private final CajaCalculoService calculoService;
    private final AuditoriaService auditoriaService;

    @Transactional(propagation = Propagation.MANDATORY)
    public CajaMovimiento crear(CajaSesion sesion, TipoMovimientoCaja tipo, MetodoPago metodo, MarcaTarjeta marca,
                                Moneda moneda, BigDecimal monto, Usuario usuario, CategoriaCaja categoria,
                                String motivo, String beneficiario, Long cuentaId, String referencia,
                                Long anulaMovimientoId, String claveIdempotencia) {
        BigDecimal exacto = monto.setScale(2, RoundingMode.HALF_UP);

        return cajaMovimientoRepository.save(CajaMovimiento.builder()
                .sesion(sesion)
                .tipo(tipo)
                .metodo(metodo)
                .marcaTarjeta(marca)
                .moneda(moneda)
                .monto(exacto)
                .tipoCambio(moneda.tipoCambio())
                .equivalentePen(moneda.aSoles(exacto))
                .categoria(categoria)
                .motivo(motivo)
                .beneficiario(beneficiario)
                .registradoPor(usuario)
                .creadoEn(Instant.now())
                .cuentaId(cuentaId)
                .referencia(referencia)
                .anulaMovimientoId(anulaMovimientoId)
                .claveIdempotencia(claveIdempotencia)
                .build());
    }

    @Transactional
    public CajaSesionDTO registrar(Long sesionId, TipoMovimientoCaja tipo,
                                   CajaRequests.Movimiento request, Usuario actor) {
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);

        Optional<CajaMovimiento> previo = cajaMovimientoRepository.findByClaveIdempotencia(request.claveIdempotencia());

        if (previo.isPresent()) {
            if (!previo.get().getSesion().getId().equals(sesionId)) {
                throw conflicto("La clave de la operación ya fue usada");
            }
            return cajaDetalleService.detalle(sesion, actor);
        }

        cajaAccesoService.exigirAbierta(sesion);

        boolean egreso = tipo == TipoMovimientoCaja.EGRESO;
        CategoriaCaja categoria = request.categoria();

        if (egreso ? !categoria.permiteEgreso() : !categoria.permiteIngreso()) {
            throw invalida("La categoría no corresponde a un " + (egreso ? "egreso" : "ingreso"));
        }

        String beneficiario = limpiar(request.beneficiario());

        if (egreso && beneficiario == null) {
            throw invalida("Indica a quién se entrega el dinero");
        }

        BigDecimal monto = request.monto().setScale(2, RoundingMode.HALF_UP);

        if (egreso) {
            exigirEfectivo(sesionId, request.moneda(), monto);
        }

        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String motivo = request.motivo().trim();

        crear(sesion, tipo, MetodoPago.EFECTIVO, null, request.moneda(), monto, usuario, categoria,
                motivo, beneficiario, null, null, null, request.claveIdempotencia());

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_MOVIMIENTO,
                null,
                tipo + " " + request.moneda() + " " + monto.toPlainString() + ";categoria=" + categoria
                        + ";tercero=" + (beneficiario == null ? "-" : beneficiario)
                        + ";caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesionId,
                motivo);

        return cajaDetalleService.detalle(sesion, actor);
    }

    @Transactional
    public CajaSesionDTO anular(Long sesionId, Long movimientoId, CajaRequests.AnularMovimiento request,
                                Usuario actor, Usuario autorizador) {
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);
        cajaAccesoService.exigirAbierta(sesion);

        CajaMovimiento original = cajaMovimientoRepository.findByIdAndSesionId(movimientoId, sesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El movimiento no existe en esta jornada"));

        if (original.getTipo() != TipoMovimientoCaja.INGRESO && original.getTipo() != TipoMovimientoCaja.EGRESO) {
            throw conflicto("Solo se pueden anular ingresos y egresos de caja");
        }
        if (original.getAnulaMovimientoId() != null) {
            throw conflicto("Ese movimiento es una anulación y no se puede anular");
        }
        if (cajaMovimientoRepository.existsByAnulaMovimientoId(movimientoId)) {
            throw conflicto("El movimiento ya fue anulado");
        }

        boolean eraIngreso = original.getTipo() == TipoMovimientoCaja.INGRESO;

        if (eraIngreso) {
            exigirEfectivo(sesionId, original.getMoneda(), original.getMonto());
        }

        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String motivo = request.motivo().trim();

        crear(sesion, eraIngreso ? TipoMovimientoCaja.EGRESO : TipoMovimientoCaja.INGRESO, original.getMetodo(),
                original.getMarcaTarjeta(), original.getMoneda(), original.getMonto(), usuario, original.getCategoria(),
                recortar("Anulación: " + motivo, 200), original.getBeneficiario(), null,
                "ANULA-" + original.getId(), original.getId(), null);

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_MOVIMIENTO_ANULADO,
                original.getTipo() + " " + original.getMoneda() + " " + original.getMonto().toPlainString()
                        + ";tercero=" + (original.getBeneficiario() == null ? "-" : original.getBeneficiario()),
                "contramovimiento;ejecuta=" + actor.getUsername() + ";autoriza=" + autorizador.getUsername()
                        + ";caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesionId,
                motivo);

        return cajaDetalleService.detalle(sesion, actor);
    }

    private void exigirEfectivo(Long sesionId, Moneda moneda, BigDecimal monto) {
        BigDecimal disponible = calculoService.saldo(sesionId, MetodoPago.EFECTIVO, null, moneda);

        if (disponible.compareTo(monto) < 0) {
            throw conflicto("El monto supera el efectivo registrado en la caja");
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private String recortar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}