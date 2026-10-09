package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstacionComanda;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.pos.Comanda;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.pos.OperacionProcesada;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.EstacionLineaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.repository.ComandaRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaMesaRepository;
import com.hafood.sistema.repository.OperacionProcesadaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hafood.sistema.domain.pos.CuentaDescuento;
import com.hafood.sistema.dto.AvisoStockDTO;
import com.hafood.sistema.dto.EnvioComandaDTO;
import com.hafood.sistema.repository.CuentaDescuentoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComandaService {

    private static final String OPERACION_ENVIAR = "ENVIAR_COMANDA";
    private static final List<EstadoLinea> RECORRIDO =
            List.of(EstadoLinea.ENVIADA, EstadoLinea.EN_PREPARACION, EstadoLinea.LISTA, EstadoLinea.ENTREGADA);
    private static final List<EstadoLinea> EN_ESTACION =
            List.of(EstadoLinea.ENVIADA, EstadoLinea.EN_PREPARACION, EstadoLinea.LISTA);
    private static final List<EstadoCuenta> CUENTAS_VIVAS =
            List.of(EstadoCuenta.ABIERTA, EstadoCuenta.PRECUENTA, EstadoCuenta.PAGO_PARCIAL);
    private static final List<EstadoLinea> AVANZADAS =
            List.of(EstadoLinea.EN_PREPARACION, EstadoLinea.LISTA, EstadoLinea.ENTREGADA);

    private final CuentaService cuentaService;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaMesaRepository cuentaMesaRepository;
    private final ComandaRepository comandaRepository;
    private final OperacionProcesadaRepository operacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;
    private final ConsumoVentaService consumoVentaService;
    private final CuentaDescuentoRepository cuentaDescuentoRepository;

    @Transactional
    public EnvioComandaDTO enviar(Long cuentaId, CuentaRequests.EnviarComanda request, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            return new EnvioComandaDTO(cuentaService.responder(cuenta), List.of());
        }

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("La cuenta ya no está abierta");
        }

        List<CuentaLinea> borradores = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId).stream()
                .filter(l -> l.getEstado() == EstadoLinea.BORRADOR)
                .toList();

        if (borradores.isEmpty()) {
            throw conflicto("No hay productos por enviar");
        }

        Instant ahora = Instant.now();
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());

        Map<EstacionComanda, List<CuentaLinea>> porEstacion = new EnumMap<>(EstacionComanda.class);
        borradores.forEach(l -> porEstacion.computeIfAbsent(estacionDe(l.getTipo()), k -> new ArrayList<>()).add(l));

        porEstacion.forEach((estacion, lineas) -> {
            Comanda comanda = comandaRepository.save(Comanda.builder()
                    .cuenta(cuenta)
                    .estacion(estacion)
                    .enviadaPor(usuario)
                    .enviadaEn(ahora)
                    .build());

            lineas.forEach(l -> {
                l.setComanda(comanda);
                l.setEstado(EstadoLinea.ENVIADA);
                l.setEnviadaEn(ahora);
            });

            auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.COMANDA_ENVIADA,
                    null,
                    "comanda=#" + comanda.getId() + ";estacion=" + estacion + ";productos="
                            + lineas.stream().map(l -> l.getCantidad() + "x " + l.getNombre()).collect(Collectors.joining(", ")),
                    null);
        });

        cuentaLineaRepository.saveAll(borradores);

        List<AvisoStockDTO> avisos = consumoVentaService.descontar(cuenta, borradores, actor);

        operacionRepository.save(OperacionProcesada.builder()
                .clave(request.claveIdempotencia())
                .tipo(OPERACION_ENVIAR)
                .cuentaId(cuentaId)
                .build());

        return new EnvioComandaDTO(cuentaService.responder(cuenta), avisos);
    }

    @Transactional
    public void cambiarEstado(Long lineaId, EstadoLinea nuevo, Usuario actor) {
        Long cuentaId = cuentaLineaRepository.findCuentaIdByLineaId(lineaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe"));

        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        CuentaLinea linea = cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .orElseThrow(() -> conflicto("El producto cambió de cuenta. Actualiza la pantalla"));

        if (linea.getEstado() == EstadoLinea.ANULADA) {
            throw conflicto("El producto fue anulado. Actualiza la pantalla");
        }

        if (!CUENTAS_VIVAS.contains(cuenta.getEstado())) {
            throw conflicto("La cuenta ya está cerrada");
        }

        int actual = RECORRIDO.indexOf(linea.getEstado());
        int destino = RECORRIDO.indexOf(nuevo);

        if (actual < 0 || destino <= actual) {
            throw conflicto("Ese cambio de estado no es válido");
        }

        Instant ahora = Instant.now();
        EstadoLinea anterior = linea.getEstado();
        linea.setEstado(nuevo);

        switch (nuevo) {
            case EN_PREPARACION -> linea.setPreparacionEn(ahora);
            case LISTA -> linea.setListaEn(ahora);
            case ENTREGADA -> linea.setEntregadaEn(ahora);
            default -> {
            }
        }

        cuentaLineaRepository.save(linea);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_ESTADO_CAMBIADO,
                linea.getNombre() + " estado=" + anterior, linea.getNombre() + " estado=" + nuevo, null);
    }

    private Long mesaCuentaId(Cuenta cuenta) {
        return cuenta.getCuentaPadre() == null ? cuenta.getId() : cuenta.getCuentaPadre().getId();
    }

    @Transactional(readOnly = true)
    public List<EstacionLineaDTO> listar(EstacionComanda estacion, Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);

        TipoCategoria tipo = estacion == EstacionComanda.BARRA ? TipoCategoria.BEBIDA : TipoCategoria.PLATO;
        List<CuentaLinea> lineas = cuentaLineaRepository.findParaEstacion(
                sedeId, tipo, EN_ESTACION, CUENTAS_VIVAS, EstadoLinea.ANULADA);

        Map<Long, String> mesas = cuentaMesaRepository.findVigentesBySedeId(sedeId).stream()
                .collect(Collectors.groupingBy(
                        cm -> cm.getCuenta().getId(),
                        Collectors.mapping(cm -> cm.getMesa().getNombre(), Collectors.joining(" + "))));

        return lineas.stream()
                .map(l -> new EstacionLineaDTO(
                        l.getId(),
                        l.getComanda() == null ? null : l.getComanda().getId(),
                        l.getCuenta().getId(),
                        mesas.getOrDefault(mesaCuentaId(l.getCuenta()), ""),
                        l.getCuenta().getMozo().getCodigo() + " " + l.getCuenta().getMozo().getNombre(),
                        l.getNombre(),
                        l.getCantidad(),
                        l.getNota(),
                        l.getEstado(),
                        l.getEnviadaEn(),
                        l.getAnuladaEn(),
                        l.getMotivoAnulacion()))
                .toList();
    }

    private EstacionComanda estacionDe(TipoCategoria tipo) {
        return tipo == TipoCategoria.BEBIDA ? EstacionComanda.BARRA : EstacionComanda.COCINA;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }



    @Transactional(readOnly = true)
    public boolean requiereAutoridad(Long cuentaId, Long lineaId, Usuario actor) {
        CuentaLinea linea = cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe en esta cuenta"));
        sedeAccesoService.exigirAcceso(actor, linea.getCuenta().getSede().getId());
        return AVANZADAS.contains(linea.getEstado());
    }

    @Transactional
    public CuentaDTO anularLinea(Long cuentaId, Long lineaId, CuentaRequests.AnularLinea request,
                                 Usuario actor, Usuario autorizador) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("Solo se pueden anular productos en una cuenta abierta");
        }

        CuentaLinea linea = cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe en esta cuenta"));
        EstadoLinea estado = linea.getEstado();

        if (estado == EstadoLinea.ANULADA) {
            throw conflicto("El producto ya fue anulado");
        }
        if (estado == EstadoLinea.BORRADOR) {
            throw conflicto("El producto aún no se envió. Quítalo directamente de la cuenta");
        }

        boolean avanzada = AVANZADAS.contains(estado);

        if (avanzada && autorizador == null) {
            throw conflicto("El producto ya avanzó en cocina o barra. Vuelve a intentarlo con la autorización de un encargado");
        }

        boolean merma = avanzada || Boolean.TRUE.equals(request.seEstabaPreparando());
        String motivo = request.motivo().trim();
        Instant ahora = Instant.now();
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String antes = "estado=" + estado + ";" + linea.getCantidad() + "x " + linea.getNombre()
                + " @ " + linea.getPrecioUnitario().toPlainString();

        linea.setEstado(EstadoLinea.ANULADA);
        linea.setAnuladaEn(ahora);
        linea.setAnuladaPor(usuario);
        linea.setMotivoAnulacion(motivo);
        linea.setAnulacionVista(false);
        linea.setMermaAnulacion(merma);
        cuentaLineaRepository.save(linea);

        consumoVentaService.revertir(cuenta, linea, merma, actor);

        List<CuentaDescuento> afectados = cuentaDescuentoRepository.findActivosByCuentaId(cuentaId).stream()
                .filter(d -> d.getLinea() != null && d.getLinea().getId().equals(lineaId))
                .toList();
        afectados.forEach(d -> {
            d.setActivo(false);
            d.setQuitadoEn(ahora);
            d.setQuitadoPor(usuario);
            d.setMotivoQuitado("Producto anulado");
        });

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_ANULADA,
                antes,
                "estado=" + EstadoLinea.ANULADA + ";merma=" + merma
                        + ";autoriza=" + (autorizador == null ? "-" : autorizador.getUsername())
                        + (afectados.isEmpty() ? "" : ";descuentos desactivados=" + afectados.size()),
                motivo);

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public void marcarAnulacionVista(Long lineaId, Usuario actor) {
        Long cuentaId = cuentaLineaRepository.findCuentaIdByLineaId(lineaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe"));

        cuentaService.bloquear(cuentaId, actor);

        CuentaLinea linea = cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .orElseThrow(() -> conflicto("El producto cambió de cuenta. Actualiza la pantalla"));

        if (linea.getEstado() != EstadoLinea.ANULADA) {
            throw conflicto("El producto no está anulado");
        }

        if (!Boolean.TRUE.equals(linea.getAnulacionVista())) {
            linea.setAnulacionVista(true);
            cuentaLineaRepository.save(linea);
        }
    }
}