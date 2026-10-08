package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCajaSesion;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoArqueo;
import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.domain.caja.Caja;
import com.hafood.sistema.domain.caja.CajaArqueo;
import com.hafood.sistema.domain.caja.CajaArqueoLinea;
import com.hafood.sistema.domain.caja.CajaSeccion;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaDTO;
import com.hafood.sistema.dto.CajaSesionDTO;
import com.hafood.sistema.dto.ConteoResultadoDTO;
import com.hafood.sistema.dto.LineaConteoDTO;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.mapper.CajaMapper;
import com.hafood.sistema.repository.CajaArqueoLineaRepository;
import com.hafood.sistema.repository.CajaArqueoRepository;
import com.hafood.sistema.repository.CajaRepository;
import com.hafood.sistema.repository.CajaSeccionRepository;
import com.hafood.sistema.repository.CajaSesionRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CajaSesionService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final int LIMITE_HISTORIAL = 300;
    private static final int MAXIMO_DIAS = 366;
    private static final Set<String> ROLES_CAJA = Set.of(
            "ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_ADMINISTRADOR", "ROLE_ENCARGADO_SEDE", "ROLE_CAJERO");

    private final CajaRepository cajaRepository;
    private final CajaSeccionRepository cajaSeccionRepository;
    private final CajaSesionRepository cajaSesionRepository;
    private final CajaArqueoRepository cajaArqueoRepository;
    private final CajaArqueoLineaRepository cajaArqueoLineaRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;
    private final CajaAccesoService cajaAccesoService;
    private final CajaCalculoService calculoService;
    private final CajaDetalleService cajaDetalleService;
    private final CajaMovimientoService cajaMovimientoService;

    public List<LineaConteoDTO> plantilla() {
        return CajaCalculoService.PLANTILLA.stream()
                .map(c -> new LineaConteoDTO(c.metodo(), c.marca(), c.moneda(), CajaCalculoService.etiqueta(c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Long sedeIdDe(Long sesionId) {
        return cajaSesionRepository.findSedeIdById(sesionId)
                .orElseThrow(() -> noExiste("La jornada de caja no existe"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public CajaSesion exigirSesionParaSeccion(Seccion seccion) {
        CajaSeccion vinculo = cajaSeccionRepository.findBySeccionIdConCaja(seccion.getId())
                .orElseThrow(() -> conflicto("La sección «" + seccion.getNombre()
                        + "» no tiene una caja asignada. Pide al encargado que la configure"));
        Caja caja = vinculo.getCaja();

        if (!caja.isActiva()) {
            throw conflicto("La caja «" + caja.getNombre() + "» está inactiva");
        }
        if (caja.getSesionAbiertaId() == null) {
            throw conflicto("La caja «" + caja.getNombre() + "» no está abierta. Ábrela antes de abrir cuentas");
        }

        CajaSesion sesion = cajaSesionRepository.findByIdForShare(caja.getSesionAbiertaId())
                .orElseThrow(() -> conflicto("La caja «" + caja.getNombre() + "» no está abierta"));

        if (sesion.getEstado() != EstadoCajaSesion.ABIERTA) {
            throw conflicto("La caja «" + caja.getNombre() + "» no está abierta. Ábrela antes de abrir cuentas");
        }

        return sesion;
    }

    @Transactional
    public CajaSesionDTO abrir(Long cajaId, CajaRequests.Abrir request, Usuario actor) {
        Caja caja = cajaRepository.findByIdForUpdate(cajaId).orElseThrow(() -> noExiste("La caja no existe"));
        Long sedeId = caja.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);

        if (!caja.isActiva()) {
            throw conflicto("La caja está inactiva");
        }
        if (caja.getSesionAbiertaId() != null) {
            throw conflicto("La caja ya está abierta");
        }
        if (!cajaSeccionRepository.existsByCajaId(cajaId)) {
            throw conflicto("La caja no tiene secciones asignadas. Pide al encargado que la configure");
        }
        if (cajaSesionRepository.existsByAbiertaPorIdAndEstado(actor.getId(), EstadoCajaSesion.ABIERTA)) {
            throw conflicto("Ya tienes una caja abierta. Ciérrala o pasa el turno antes de abrir otra");
        }

        Map<Moneda, CajaRequests.LineaConteo> porMoneda = new EnumMap<>(Moneda.class);

        for (CajaRequests.LineaConteo linea : request.fondos()) {
            if (linea.metodo() != MetodoPago.EFECTIVO) {
                throw invalida("El fondo inicial solo puede ser en efectivo");
            }
            if (porMoneda.put(linea.moneda(), linea) != null) {
                throw invalida("Hay una moneda repetida en el fondo inicial");
            }
        }

        List<CajaCalculoService.LineaEvaluada> fondos = new ArrayList<>();

        for (Moneda moneda : Moneda.values()) {
            CajaCalculoService.Clave clave = new CajaCalculoService.Clave(MetodoPago.EFECTIVO, null, moneda);
            CajaRequests.LineaConteo linea = porMoneda.get(moneda);
            CajaCalculoService.Contado contado = linea == null
                    ? new CajaCalculoService.Contado(BigDecimal.ZERO.setScale(2), null)
                    : calculoService.contar(clave, linea);
            fondos.add(new CajaCalculoService.LineaEvaluada(clave, contado.total(), contado.total(),
                    BigDecimal.ZERO.setScale(2), contado.detalle()));
        }

        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        CajaSesion sesion = cajaSesionRepository.save(CajaSesion.builder()
                .caja(caja)
                .sede(caja.getSede())
                .abiertaPor(usuario)
                .build());

        caja.setSesionAbiertaId(sesion.getId());
        cajaRepository.saveAndFlush(caja);

        for (CajaCalculoService.LineaEvaluada fondo : fondos) {
            cajaMovimientoService.crear(sesion, TipoMovimientoCaja.APERTURA, MetodoPago.EFECTIVO, null,
                    fondo.clave().moneda(), fondo.contado(), usuario, null, "Fondo inicial", null, null,
                    "APERTURA-" + sesion.getId(), null, null);
        }

        guardarArqueo(sesion, TipoArqueo.APERTURA, usuario,
                new CajaCalculoService.Evaluacion(fondos, false, 0, BigDecimal.ZERO), null, null);

        auditoriaService.registrar(actor, sedeId, null, AccionAuditoria.CAJA_ABIERTA, null,
                "caja=" + caja.getNombre() + ";sesion=#" + sesion.getId() + ";fondo=" + resumenFondo(fondos), null);

        return cajaDetalleService.detalle(sesion, actor);
    }

    @Transactional(readOnly = true)
    public CajaSesionDTO obtener(Long sesionId, Usuario actor) {
        CajaSesion sesion = cajaSesionRepository.findById(sesionId)
                .orElseThrow(() -> noExiste("La jornada de caja no existe"));
        sedeAccesoService.exigirAcceso(actor, sesion.getSede().getId());
        cajaAccesoService.exigirOperador(sesion, actor);
        return cajaDetalleService.detalle(sesion, actor);
    }

    @Transactional(readOnly = true)
    public List<CajaDTO.SesionResumen> historial(Long sedeId, LocalDate desde, LocalDate hasta, Long cajaId,
                                                 boolean soloPendientes, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        boolean supervisor = autorizacionService.esSupervisor(actor);

        LocalDate fin = hasta != null ? hasta : LocalDate.now(ZONA);
        LocalDate inicio = desde != null ? desde : fin.minusDays(6);

        if (inicio.isAfter(fin)) {
            throw invalida("La fecha inicial no puede ser posterior a la final");
        }
        if (inicio.plusDays(MAXIMO_DIAS).isBefore(fin)) {
            throw invalida("El rango no puede superar un año");
        }

        Instant desdeInstant = inicio.atStartOfDay(ZONA).toInstant();
        Instant hastaInstant = fin.plusDays(1).atStartOfDay(ZONA).toInstant();

        return cajaSesionRepository.findHistorial(sedeId, desdeInstant, hastaInstant, PageRequest.of(0, LIMITE_HISTORIAL))
                .stream()
                .filter(s -> cajaId == null || s.getCaja().getId().equals(cajaId))
                .filter(s -> !soloPendientes || s.isDiferenciaPendiente())
                .filter(s -> supervisor || s.getAbiertaPor().getId().equals(actor.getId()))
                .map(s -> CajaMapper.toResumen(s, supervisor))
                .toList();
    }

    @Transactional
    public ConteoResultadoDTO arqueoParcial(Long sesionId, CajaRequests.Arqueo request, Usuario actor) {
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);
        cajaAccesoService.exigirAbierta(sesion);

        CajaCalculoService.Evaluacion evaluacion = calculoService.evaluar(sesionId, request.lineas());
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());

        guardarArqueo(sesion, TipoArqueo.PARCIAL, usuario, evaluacion, limpiar(request.observacion()), null);

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_ARQUEO, null,
                "arqueo parcial;caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesionId
                        + ";lineasConDiferencia=" + evaluacion.lineasConDiferencia()
                        + ";diferenciaPen=" + evaluacion.diferenciaPen().toPlainString(), null);

        return new ConteoResultadoDTO(false, evaluacion.hayDiferencia(), cajaDetalleService.detalle(sesion, actor), null);
    }

    @Transactional
    public ConteoResultadoDTO cerrar(Long sesionId, CajaRequests.Cierre request, Usuario actor) {
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);
        cajaAccesoService.exigirAbierta(sesion);

        long pendientes = cuentaRepository.countByCajaSesionIdAndEstadoIn(sesionId, CajaDetalleService.CUENTAS_PENDIENTES);

        if (pendientes > 0) {
            throw conflicto("Hay " + pendientes + (pendientes == 1 ? " cuenta" : " cuentas")
                    + " sin cerrar en esta caja. Cóbralas o anúlalas antes de cerrar, o usa el pase de turno");
        }

        CajaCalculoService.Evaluacion evaluacion = calculoService.evaluar(sesionId, request.lineas());
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String motivo = limpiar(request.motivoDiferencia());

        if (evaluacion.hayDiferencia() && !(request.confirmarDiferencia() && motivo != null)) {
            if (request.confirmarDiferencia()) {
                throw invalida("Escribe el motivo de la diferencia para cerrar");
            }
            registrarIntento(sesion, usuario, actor, evaluacion, request.observacion());
            return new ConteoResultadoDTO(false, true, null, null);
        }

        guardarArqueo(sesion, TipoArqueo.CIERRE, usuario, evaluacion, limpiar(request.observacion()), motivo);
        finalizar(sesion, usuario, TipoArqueo.CIERRE, evaluacion, limpiar(request.entregadoA()));

        Caja caja = cajaRepository.findByIdForUpdate(sesion.getCaja().getId())
                .orElseThrow(() -> noExiste("La caja no existe"));
        caja.setSesionAbiertaId(null);
        cajaRepository.save(caja);

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_CERRADA,
                "estado=" + EstadoCajaSesion.ABIERTA,
                "estado=" + EstadoCajaSesion.CERRADA + ";caja=" + caja.getNombre() + ";sesion=#" + sesionId
                        + ";lineasConDiferencia=" + evaluacion.lineasConDiferencia()
                        + ";diferenciaPen=" + evaluacion.diferenciaPen().toPlainString()
                        + ";entregadoA=" + (sesion.getEntregadoA() == null ? "-" : sesion.getEntregadoA()),
                motivo);

        return new ConteoResultadoDTO(true, evaluacion.hayDiferencia(), cajaDetalleService.detalle(sesion, actor), null);
    }

    @Transactional
    public ConteoResultadoDTO pasarTurno(Long sesionId, CajaRequests.PaseTurno request, Usuario actor, Usuario receptor) {
        CajaSesion sesion = cajaAccesoService.bloquear(sesionId, actor);
        cajaAccesoService.exigirAbierta(sesion);
        Long sedeId = sesion.getSede().getId();

        validarReceptor(receptor, actor, sedeId);

        CajaCalculoService.Evaluacion evaluacion = calculoService.evaluar(sesionId, request.lineas());
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String motivo = limpiar(request.motivoDiferencia());

        if (evaluacion.hayDiferencia() && !(request.confirmarDiferencia() && motivo != null)) {
            if (request.confirmarDiferencia()) {
                throw invalida("Escribe el motivo de la diferencia para pasar el turno");
            }
            registrarIntento(sesion, usuario, actor, evaluacion, request.observacion());
            return new ConteoResultadoDTO(false, true, null, null);
        }

        guardarArqueo(sesion, TipoArqueo.PASE_TURNO, usuario, evaluacion, limpiar(request.observacion()), motivo);
        finalizar(sesion, usuario, TipoArqueo.PASE_TURNO, evaluacion, receptor.getUsername());

        CajaSesion nueva = cajaSesionRepository.save(CajaSesion.builder()
                .caja(sesion.getCaja())
                .sede(sesion.getSede())
                .abiertaPor(usuarioRepository.getReferenceById(receptor.getId()))
                .turnoAnteriorId(sesion.getId())
                .build());

        Caja caja = cajaRepository.findByIdForUpdate(sesion.getCaja().getId())
                .orElseThrow(() -> noExiste("La caja no existe"));
        caja.setSesionAbiertaId(nueva.getId());
        cajaRepository.saveAndFlush(caja);

        for (CajaCalculoService.LineaEvaluada linea : evaluacion.lineas()) {
            if (linea.clave().metodo() == MetodoPago.EFECTIVO) {
                cajaMovimientoService.crear(nueva, TipoMovimientoCaja.APERTURA, MetodoPago.EFECTIVO, null,
                        linea.clave().moneda(), linea.contado(), usuario, null,
                        "Efectivo recibido en el pase de turno", null, null,
                        "PASE_TURNO-" + sesion.getId(), null, null);
            }
        }

        List<Long> cuentaIds = cuentaRepository.findIdsByCajaSesionIdAndEstadoIn(sesionId, CajaDetalleService.CUENTAS_PENDIENTES);

        for (Long cuentaId : cuentaIds) {
            Cuenta cuenta = cuentaRepository.findByIdForUpdate(cuentaId)
                    .orElseThrow(() -> noExiste("La cuenta no existe"));
            cuenta.setCajaSesion(nueva);
            cuentaRepository.save(cuenta);
            auditoriaService.registrar(actor, sedeId, cuentaId, AccionAuditoria.CUENTA_CAJA_REASIGNADA,
                    "sesion=#" + sesionId, "sesion=#" + nueva.getId(), "Pase de turno");
        }

        auditoriaService.registrar(actor, sedeId, null, AccionAuditoria.CAJA_TURNO_PASADO,
                "cajera=" + actor.getUsername() + ";sesion=#" + sesionId,
                "cajera=" + receptor.getUsername() + ";sesion=#" + nueva.getId() + ";caja=" + caja.getNombre()
                        + ";cuentasPasadas=" + cuentaIds.size()
                        + ";lineasConDiferencia=" + evaluacion.lineasConDiferencia()
                        + ";diferenciaPen=" + evaluacion.diferenciaPen().toPlainString(),
                motivo);

        return new ConteoResultadoDTO(true, evaluacion.hayDiferencia(), cajaDetalleService.detalle(sesion, actor), nueva.getId());
    }

    @Transactional
    public CajaSesionDTO revisar(Long sesionId, CajaRequests.Revisar request, Usuario actor) {
        CajaSesion sesion = cajaSesionRepository.findByIdForUpdate(sesionId)
                .orElseThrow(() -> noExiste("La jornada de caja no existe"));
        sedeAccesoService.exigirAcceso(actor, sesion.getSede().getId());

        if (!autorizacionService.esSupervisor(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un encargado o administrador puede revisar una diferencia de caja");
        }
        if (sesion.getEstado() != EstadoCajaSesion.CERRADA) {
            throw conflicto("La jornada aún está abierta");
        }
        if (!sesion.isDiferenciaPendiente()) {
            throw conflicto("Esta jornada no tiene diferencias por revisar");
        }
        if (sesion.getCerradaPor() != null && sesion.getCerradaPor().getId().equals(actor.getId())) {
            throw conflicto("La diferencia debe revisarla otra persona distinta a quien cerró la caja");
        }

        sesion.setDiferenciaPendiente(false);
        sesion.setRevisadaPor(usuarioRepository.getReferenceById(actor.getId()));
        sesion.setRevisadaEn(Instant.now());
        sesion.setComentarioRevision(request.comentario().trim());
        cajaSesionRepository.save(sesion);

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_REVISADA,
                "diferenciaPendiente=true;sesion=#" + sesionId,
                "diferenciaPen=" + (sesion.getDiferenciaPen() == null ? "0" : sesion.getDiferenciaPen().toPlainString())
                        + ";revisa=" + actor.getUsername(),
                request.comentario().trim());

        return cajaDetalleService.detalle(sesion, actor);
    }

    private void registrarIntento(CajaSesion sesion, Usuario usuario, Usuario actor,
                                  CajaCalculoService.Evaluacion evaluacion, String observacion) {
        guardarArqueo(sesion, TipoArqueo.INTENTO_CIERRE, usuario, evaluacion, limpiar(observacion), null);

        auditoriaService.registrar(actor, sesion.getSede().getId(), null, AccionAuditoria.CAJA_ARQUEO, null,
                "intento de cierre con diferencia;caja=" + sesion.getCaja().getNombre() + ";sesion=#" + sesion.getId()
                        + ";lineasConDiferencia=" + evaluacion.lineasConDiferencia()
                        + ";diferenciaPen=" + evaluacion.diferenciaPen().toPlainString(), null);
    }

    private void finalizar(CajaSesion sesion, Usuario usuario, TipoArqueo tipo,
                           CajaCalculoService.Evaluacion evaluacion, String entregadoA) {
        sesion.setEstado(EstadoCajaSesion.CERRADA);
        sesion.setCerradaPor(usuario);
        sesion.setCerradaEn(Instant.now());
        sesion.setTipoCierre(tipo);
        sesion.setEntregadoA(entregadoA);
        sesion.setDiferenciaPendiente(evaluacion.hayDiferencia());
        sesion.setDiferenciaPen(evaluacion.diferenciaPen().setScale(2, RoundingMode.HALF_UP));
        sesion.setLineasConDiferencia(evaluacion.lineasConDiferencia());
        cajaSesionRepository.save(sesion);
    }

    private void validarReceptor(Usuario receptor, Usuario actor, Long sedeId) {
        if (receptor.getId().equals(actor.getId())) {
            throw invalida("Quien recibe la caja debe ser otra persona");
        }
        if (receptor.getAuthorities().stream().noneMatch(a -> ROLES_CAJA.contains(a.getAuthority()))) {
            throw invalida("Esa persona no puede operar una caja");
        }

        try {
            sedeAccesoService.exigirAcceso(receptor, sedeId);
        } catch (ResponseStatusException e) {
            throw invalida("Quien recibe la caja no pertenece a esta sede");
        }

        if (cajaSesionRepository.existsByAbiertaPorIdAndEstado(receptor.getId(), EstadoCajaSesion.ABIERTA)) {
            throw conflicto("Quien recibe la caja ya tiene otra caja abierta");
        }
    }

    private void guardarArqueo(CajaSesion sesion, TipoArqueo tipo, Usuario usuario,
                               CajaCalculoService.Evaluacion evaluacion, String observacion, String motivoDiferencia) {
        CajaArqueo arqueo = cajaArqueoRepository.save(CajaArqueo.builder()
                .sesion(sesion)
                .tipo(tipo)
                .realizadoPor(usuario)
                .realizadoEn(Instant.now())
                .observacion(observacion)
                .motivoDiferencia(motivoDiferencia)
                .hayDiferencia(evaluacion.hayDiferencia())
                .build());

        cajaArqueoLineaRepository.saveAll(evaluacion.lineas().stream()
                .map(l -> CajaArqueoLinea.builder()
                        .arqueo(arqueo)
                        .metodo(l.clave().metodo())
                        .marcaTarjeta(l.clave().marca())
                        .moneda(l.clave().moneda())
                        .esperado(l.esperado())
                        .contado(l.contado())
                        .diferencia(l.diferencia())
                        .detalle(l.detalle())
                        .build())
                .toList());
    }

    private String resumenFondo(List<CajaCalculoService.LineaEvaluada> fondos) {
        StringBuilder texto = new StringBuilder();

        for (CajaCalculoService.LineaEvaluada f : fondos) {
            if (!texto.isEmpty()) {
                texto.append(", ");
            }
            texto.append(f.clave().moneda()).append(' ').append(f.contado().toPlainString());
        }

        return texto.toString();
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private ResponseStatusException noExiste(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}