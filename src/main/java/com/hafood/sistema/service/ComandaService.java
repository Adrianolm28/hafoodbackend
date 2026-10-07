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

    private final CuentaService cuentaService;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaMesaRepository cuentaMesaRepository;
    private final ComandaRepository comandaRepository;
    private final OperacionProcesadaRepository operacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public CuentaDTO enviar(Long cuentaId, CuentaRequests.EnviarComanda request, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            return cuentaService.responder(cuenta);
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

        operacionRepository.save(OperacionProcesada.builder()
                .clave(request.claveIdempotencia())
                .tipo(OPERACION_ENVIAR)
                .cuentaId(cuentaId)
                .build());

        return cuentaService.responder(cuenta);
    }

    @Transactional
    public void cambiarEstado(Long lineaId, EstadoLinea nuevo, Usuario actor) {
        Long cuentaId = cuentaLineaRepository.findCuentaIdByLineaId(lineaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe"));

        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);

        CuentaLinea linea = cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .orElseThrow(() -> conflicto("El producto cambió de cuenta. Actualiza la pantalla"));

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

    @Transactional(readOnly = true)
    public List<EstacionLineaDTO> listar(EstacionComanda estacion, Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);

        TipoCategoria tipo = estacion == EstacionComanda.BARRA ? TipoCategoria.BEBIDA : TipoCategoria.PLATO;
        List<CuentaLinea> lineas = cuentaLineaRepository.findParaEstacion(sedeId, tipo, EN_ESTACION, CUENTAS_VIVAS);

        Map<Long, String> mesas = cuentaMesaRepository.findVigentesBySedeId(sedeId).stream()
                .collect(Collectors.groupingBy(
                        cm -> cm.getCuenta().getId(),
                        Collectors.mapping(cm -> cm.getMesa().getNombre(), Collectors.joining(" + "))));

        return lineas.stream()
                .map(l -> new EstacionLineaDTO(
                        l.getId(),
                        l.getComanda() == null ? null : l.getComanda().getId(),
                        l.getCuenta().getId(),
                        mesas.getOrDefault(l.getCuenta().getId(), ""),
                        l.getCuenta().getMozo().getCodigo() + " " + l.getCuenta().getMozo().getNombre(),
                        l.getNombre(),
                        l.getCantidad(),
                        l.getNota(),
                        l.getEstado(),
                        l.getEnviadaEn()))
                .toList();
    }

    private EstacionComanda estacionDe(TipoCategoria tipo) {
        return tipo == TipoCategoria.BEBIDA ? EstacionComanda.BARRA : EstacionComanda.COCINA;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}