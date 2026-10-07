package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.TipoPersonal;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.estructura.Mesa;
import com.hafood.sistema.domain.estructura.Personal;
import com.hafood.sistema.domain.pos.*;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.request.CuentaRequests;
import com.hafood.sistema.mapper.CuentaMapper;
import com.hafood.sistema.repository.CartaRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaMesaRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.MesaRepository;
import com.hafood.sistema.repository.OperacionProcesadaRepository;
import com.hafood.sistema.repository.PersonalRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.hafood.sistema.repository.CuentaDescuentoRepository;
import com.hafood.sistema.domain.pos.CuentaDescuento;
@Service
@RequiredArgsConstructor
public class CuentaService {

    private static final int CANTIDAD_MAXIMA = 99;
    private static final String OPERACION_AGREGAR_LINEA = "AGREGAR_LINEA";
    private static final EnumSet<EstadoCuenta> ESTADOS_VIVOS =
            EnumSet.of(EstadoCuenta.ABIERTA, EstadoCuenta.PRECUENTA, EstadoCuenta.PAGO_PARCIAL);

    private final CuentaRepository cuentaRepository;
    private final CuentaMesaRepository cuentaMesaRepository;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final OperacionProcesadaRepository operacionRepository;
    private final MesaRepository mesaRepository;
    private final CartaRepository cartaRepository;
    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final CartaMenuService cartaMenuService;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;
    private final CuentaCalculoService calculoService;
    private final CuentaDescuentoRepository cuentaDescuentoRepository;

    @Transactional(readOnly = true)
    public CuentaDTO obtener(Long id, Usuario actor) {
        Cuenta cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> noExiste("La cuenta no existe"));
        sedeAccesoService.exigirAcceso(actor, cuenta.getSede().getId());
        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO abrir(CuentaRequests.Abrir request, Usuario actor) {
        Mesa mesa = mesaRepository.findByIdForUpdate(request.mesaId())
                .orElseThrow(() -> noExiste("La mesa no existe"));
        Long sedeId = mesa.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);
        exigirMesaOperativa(mesa);

        if (cuentaMesaRepository.existsByMesaIdAndHastaIsNull(mesa.getId())) {
            throw conflicto("La mesa ya tiene una cuenta abierta");
        }

        Carta carta = cartaRepository.findById(request.cartaId())
                .orElseThrow(() -> noExiste("La carta no existe"));

        if (!carta.getSede().getId().equals(sedeId)) {
            throw invalida("La carta no pertenece a la sede de la mesa");
        }
        if (!carta.isActivo()) {
            throw conflicto("La carta está inactiva");
        }

        Personal mozo = buscarMozo(request.mozoId(), sedeId);
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        Instant ahora = Instant.now();

        Cuenta cuenta = cuentaRepository.save(Cuenta.builder()
                .sede(mesa.getSede())
                .carta(carta)
                .mozo(mozo)
                .abiertaPor(usuario)
                .comensales(request.comensales())
                .nota(limpiar(request.nota()))
                .build());

        abrirFila(cuenta, mesa, usuario, ahora);

        auditoriaService.registrar(actor, sedeId, cuenta.getId(), AccionAuditoria.CUENTA_ABIERTA, null,
                "mesa=" + mesa.getNombre() + ";mozo=" + mozo.getCodigo() + " " + mozo.getNombre()
                        + ";carta=" + carta.getNombre(), null);

        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO agregarLinea(Long cuentaId, CuentaRequests.AgregarLinea request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            return responder(cuenta);
        }

        exigirEditable(cuenta);

        CartaMenuService.ProductoCarta producto =
                cartaMenuService.resolver(cuenta.getCarta().getId(), request.tipo(), request.productoId());

        if (producto.agotado()) {
            throw conflicto("«" + producto.nombre() + "» está agotado");
        }
        if (producto.precio() == null || producto.precio().signum() <= 0) {
            throw conflicto("El producto no tiene un precio válido en esta carta");
        }

        String nota = limpiar(request.nota());
        BigDecimal precio = producto.precio().setScale(2, RoundingMode.HALF_UP);
        List<CuentaLinea> lineas = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId);

        CuentaLinea existente = lineas.stream()
                .filter(l -> l.getEstado() == EstadoLinea.BORRADOR
                        && l.getTipo() == request.tipo()
                        && l.getProductoId().equals(producto.productoId())
                        && Objects.equals(l.getNota(), nota)
                        && l.getPrecioUnitario().compareTo(precio) == 0)
                .findFirst()
                .orElse(null);

        if (existente != null) {
            int nueva = existente.getCantidad() + request.cantidad();

            if (nueva > CANTIDAD_MAXIMA) {
                throw conflicto("Máximo " + CANTIDAD_MAXIMA + " unidades por línea");
            }

            String antes = describir(existente);
            existente.setCantidad(nueva);
            cuentaLineaRepository.save(existente);
            auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_MODIFICADA,
                    antes, describir(existente), null);
        } else {
            CuentaLinea linea = cuentaLineaRepository.save(CuentaLinea.builder()
                    .cuenta(cuenta)
                    .tipo(request.tipo())
                    .productoId(producto.productoId())
                    .nombre(producto.nombre())
                    .cantidad(request.cantidad())
                    .precioUnitario(precio)
                    .nota(nota)
                    .cartaId(cuenta.getCarta().getId())
                    .agregadaPor(usuarioRepository.getReferenceById(actor.getId()))
                    .build());
            auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_AGREGADA,
                    null, describir(linea), null);
        }

        operacionRepository.save(OperacionProcesada.builder()
                .clave(request.claveIdempotencia())
                .tipo(OPERACION_AGREGAR_LINEA)
                .cuentaId(cuentaId)
                .build());

        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO actualizarLinea(Long cuentaId, Long lineaId, CuentaRequests.ActualizarLinea request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);
        exigirEditable(cuenta);

        CuentaLinea linea = buscarLinea(lineaId, cuentaId);
        exigirBorrador(linea);

        String antes = describir(linea);
        linea.setCantidad(request.cantidad());
        linea.setNota(limpiar(request.nota()));
        cuentaLineaRepository.save(linea);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_MODIFICADA,
                antes, describir(linea), null);

        return responder(cuenta);
    }

    @Transactional
    public void eliminarLinea(Long cuentaId, Long lineaId, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);
        exigirEditable(cuenta);

        CuentaLinea linea = buscarLinea(lineaId, cuentaId);
        exigirBorrador(linea);

        String antes = describir(linea);
        linea.setEstado(EstadoLinea.ANULADA);
        cuentaLineaRepository.save(linea);

        Instant ahora = Instant.now();
        Usuario usuario = usuarioRef(actor);
        List<CuentaDescuento> afectados = cuentaDescuentoRepository.findActivosByCuentaId(cuentaId).stream()
                .filter(d -> d.getLinea() != null && d.getLinea().getId().equals(lineaId))
                .toList();
        afectados.forEach(d -> {
            d.setActivo(false);
            d.setQuitadoEn(ahora);
            d.setQuitadoPor(usuario);
            d.setMotivoQuitado("Producto quitado de la cuenta");
        });

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.LINEA_ELIMINADA,
                antes, afectados.isEmpty() ? null : "descuentos desactivados=" + afectados.size(), null);

        calculoService.recalcular(cuenta);
        cuentaRepository.save(cuenta);
    }

    @Transactional
    public CuentaDTO anular(Long cuentaId, CuentaRequests.Anular request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("Solo se puede anular una cuenta abierta y sin pagos");
        }

        List<CuentaLinea> lineas = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId);
        boolean hayEnviadas = lineas.stream()
                .anyMatch(l -> l.getEstado() != EstadoLinea.BORRADOR && l.getEstado() != EstadoLinea.ANULADA);

        if (hayEnviadas) {
            throw conflicto("La cuenta tiene productos enviados. Anula esos productos primero");
        }

        Instant ahora = Instant.now();

        String antes = "estado=" + cuenta.getEstado() + ";total=" + cuenta.getTotal().toPlainString();

        List<CuentaMesa> filas = cuentaMesaRepository.findVigentesByCuentaId(cuentaId);
        filas.forEach(f -> {
            cerrarFila(f, ahora);
            liberarSiTemporal(f.getMesa());
        });
        cuentaMesaRepository.saveAllAndFlush(filas);

        cuenta.setEstado(EstadoCuenta.ANULADA);
        cuenta.setCerradaEn(ahora);
        cuenta.setMotivoAnulacion(request.motivo().trim());
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.CUENTA_ANULADA,
                antes, "estado=" + EstadoCuenta.ANULADA, request.motivo().trim());

        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO cambiarMozo(Long cuentaId, CuentaRequests.CambiarMozo request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);
        exigirViva(cuenta);

        Personal nuevo = buscarMozo(request.mozoId(), cuenta.getSede().getId());
        Personal actual = cuenta.getMozo();

        if (actual.getId().equals(nuevo.getId())) {
            return responder(cuenta);
        }

        String antes = "mozo=" + actual.getCodigo() + " " + actual.getNombre();
        cuenta.setMozo(nuevo);
        cuentaRepository.save(cuenta);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.MOZO_CAMBIADO,
                antes, "mozo=" + nuevo.getCodigo() + " " + nuevo.getNombre(), null);

        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO cambiarMesa(Long cuentaId, CuentaRequests.CambiarMesa request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);
        exigirViva(cuenta);

        if (request.mesaOrigenId().equals(request.mesaDestinoId())) {
            throw invalida("La mesa de destino debe ser distinta a la de origen");
        }

        List<CuentaMesa> filas = cuentaMesaRepository.findVigentesByCuentaId(cuentaId);
        CuentaMesa filaOrigen = filas.stream()
                .filter(f -> f.getMesa().getId().equals(request.mesaOrigenId()))
                .findFirst()
                .orElseThrow(() -> conflicto("La cuenta no está en esa mesa"));

        Mesa destino = mesaRepository.findByIdForUpdate(request.mesaDestinoId())
                .orElseThrow(() -> noExiste("La mesa de destino no existe"));

        if (!destino.getSede().getId().equals(cuenta.getSede().getId())) {
            throw invalida("La mesa de destino pertenece a otra sede");
        }
        exigirMesaOperativa(destino);

        if (cuentaMesaRepository.existsByMesaIdAndHastaIsNull(destino.getId())) {
            throw conflicto("La mesa de destino está ocupada");
        }

        Instant ahora = Instant.now();
        Mesa origen = filaOrigen.getMesa();
        String antes = "mesa=" + origen.getNombre();

        cerrarFila(filaOrigen, ahora);
        cuentaMesaRepository.saveAndFlush(filaOrigen);
        liberarSiTemporal(origen);
        abrirFila(cuenta, destino, usuarioRef(actor), ahora);

        auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.MESA_CAMBIADA,
                antes, "mesa=" + destino.getNombre(), null);

        return responder(cuenta);
    }

    @Transactional
    public CuentaDTO juntarMesa(Long cuentaId, CuentaRequests.JuntarMesa request, Usuario actor) {
        Cuenta cuenta = bloquear(cuentaId, actor);
        exigirViva(cuenta);

        Mesa mesa = mesaRepository.findByIdForUpdate(request.mesaId())
                .orElseThrow(() -> noExiste("La mesa no existe"));

        if (!mesa.getSede().getId().equals(cuenta.getSede().getId())) {
            throw invalida("La mesa pertenece a otra sede");
        }
        exigirMesaOperativa(mesa);

        Optional<Long> otraCuentaId = cuentaMesaRepository.findCuentaIdVigenteByMesaId(mesa.getId());

        if (otraCuentaId.isEmpty()) {
            List<CuentaMesa> actuales = cuentaMesaRepository.findVigentesByCuentaId(cuentaId);
            String antes = "mesas=" + nombres(actuales.stream().map(CuentaMesa::getMesa).toList());
            abrirFila(cuenta, mesa, usuarioRef(actor), Instant.now());

            auditoriaService.registrar(actor, cuenta.getSede().getId(), cuentaId, AccionAuditoria.MESA_AGREGADA,
                    antes, antes + ", " + mesa.getNombre(), null);

            return responder(cuenta);
        }

        if (otraCuentaId.get().equals(cuentaId)) {
            throw conflicto("La mesa ya pertenece a esta cuenta");
        }

        return fusionar(cuenta, otraCuentaId.get(), actor);
    }

    private CuentaDTO fusionar(Cuenta destino, Long origenId, Usuario actor) {
        Cuenta origen = cuentaRepository.findByIdForUpdate(origenId)
                .orElseThrow(() -> noExiste("La cuenta a unir no existe"));

        if (origen.getEstado() != EstadoCuenta.ABIERTA || destino.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("Solo se pueden unir cuentas abiertas y sin pagos");
        }
        if (!origen.getSede().getId().equals(destino.getSede().getId())) {
            throw invalida("Las cuentas pertenecen a sedes distintas");
        }
        if (cuentaDescuentoRepository.existsByCuentaIdAndLineaIsNullAndActivoTrue(origenId)
                || cuentaDescuentoRepository.existsByCuentaIdAndLineaIsNullAndActivoTrue(destino.getId())) {
            throw conflicto("Una de las cuentas tiene un descuento general. Quítalo antes de juntarlas");
        }

        calculoService.recalcular(origen);
        calculoService.recalcular(destino);
        BigDecimal totalOrigen = origen.getTotal();
        BigDecimal totalDestinoAntes = destino.getTotal();

        Instant ahora = Instant.now();
        Usuario usuario = usuarioRef(actor);

        List<CuentaMesa> filasOrigen = cuentaMesaRepository.findVigentesByCuentaId(origenId);
        List<Mesa> mesasOrigen = filasOrigen.stream().map(CuentaMesa::getMesa).toList();
        List<CuentaMesa> filasDestino = cuentaMesaRepository.findVigentesByCuentaId(destino.getId());
        String mesasDestino = nombres(filasDestino.stream().map(CuentaMesa::getMesa).toList());
        List<CuentaLinea> lineasOrigen = cuentaLineaRepository.findByCuentaIdConUsuario(origenId);

        filasOrigen.forEach(f -> cerrarFila(f, ahora));
        cuentaMesaRepository.saveAllAndFlush(filasOrigen);
        mesasOrigen.forEach(m -> abrirFila(destino, m, usuario, ahora));

        lineasOrigen.forEach(l -> {
            if (l.getCuentaOrigen() == null) {
                l.setCuentaOrigen(origen);
            }
            l.setCuenta(destino);
        });
        cuentaLineaRepository.saveAll(lineasOrigen);

        cuentaDescuentoRepository.findByCuentaId(origenId).stream()
                .filter(d -> d.getLinea() != null)
                .forEach(d -> d.setCuenta(destino));

        origen.setEstado(EstadoCuenta.FUSIONADA);
        origen.setFusionadaEn(destino);
        origen.setCerradaEn(ahora);
        origen.setTotal(BigDecimal.ZERO);
        origen.setSubtotal(BigDecimal.ZERO);
        origen.setDescuentoTotal(BigDecimal.ZERO);
        cuentaRepository.save(origen);

        Long sedeId = destino.getSede().getId();
        String mesasOrigenTexto = nombres(mesasOrigen);

        auditoriaService.registrar(actor, sedeId, destino.getId(), AccionAuditoria.CUENTAS_UNIDAS,
                "mesas=" + mesasDestino + ";total=" + totalDestinoAntes.toPlainString(),
                "se unió la cuenta #" + origenId + " (mesas=" + mesasOrigenTexto + ";total="
                        + totalOrigen.toPlainString() + ")", null);
        auditoriaService.registrar(actor, sedeId, origenId, AccionAuditoria.CUENTA_FUSIONADA,
                "estado=" + EstadoCuenta.ABIERTA + ";mesas=" + mesasOrigenTexto,
                "estado=" + EstadoCuenta.FUSIONADA + " en la cuenta #" + destino.getId(), null);

        return responder(destino);
    }

    public Cuenta bloquear(Long cuentaId, Usuario actor) {
        Cuenta cuenta = cuentaRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> noExiste("La cuenta no existe"));
        sedeAccesoService.exigirAcceso(actor, cuenta.getSede().getId());
        return cuenta;
    }

    public CuentaDTO responder(Cuenta cuenta) {
        CuentaCalculoService.Resultado resultado = calculoService.recalcular(cuenta);
        List<CuentaMesa> mesas = cuentaMesaRepository.findVigentesByCuentaId(cuenta.getId());
        return CuentaMapper.toDTO(cuenta, mesas, resultado);
    }

    private static BigDecimal sumar(List<CuentaLinea> lineas) {
        return lineas.stream()
                .filter(l -> l.getEstado() != EstadoLinea.ANULADA)
                .map(l -> l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void abrirFila(Cuenta cuenta, Mesa mesa, Usuario usuario, Instant ahora) {
        try {
            cuentaMesaRepository.saveAndFlush(CuentaMesa.builder()
                    .cuenta(cuenta)
                    .mesa(mesa)
                    .desde(ahora)
                    .mesaVigenteId(mesa.getId())
                    .movidaPor(usuario)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw conflicto("La mesa ya tiene una cuenta abierta");
        }
    }

    private void cerrarFila(CuentaMesa fila, Instant ahora) {
        fila.setHasta(ahora);
        fila.setMesaVigenteId(null);
    }

    private void liberarSiTemporal(Mesa mesa) {
        if (Boolean.TRUE.equals(mesa.getTemporal())) {
            mesa.setActiva(false);
        }
    }

    private Personal buscarMozo(Long mozoId, Long sedeId) {
        Personal personal = personalRepository.findById(mozoId)
                .orElseThrow(() -> noExiste("El mozo no existe"));

        if (!personal.getSede().getId().equals(sedeId)) {
            throw invalida("El mozo no pertenece a esta sede");
        }
        if (personal.getTipo() != TipoPersonal.MOZO) {
            throw invalida("El personal seleccionado no es mozo");
        }
        if (!Boolean.TRUE.equals(personal.getActivo())) {
            throw conflicto("El mozo está inactivo");
        }

        return personal;
    }

    private CuentaLinea buscarLinea(Long lineaId, Long cuentaId) {
        return cuentaLineaRepository.findByIdAndCuentaId(lineaId, cuentaId)
                .filter(l -> l.getEstado() != EstadoLinea.ANULADA)
                .orElseThrow(() -> noExiste("El producto no existe en esta cuenta"));
    }

    private Usuario usuarioRef(Usuario actor) {
        return usuarioRepository.getReferenceById(actor.getId());
    }

    private void exigirEditable(Cuenta cuenta) {
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw conflicto("La cuenta ya no está abierta para modificar productos");
        }
    }

    private void exigirViva(Cuenta cuenta) {
        if (!ESTADOS_VIVOS.contains(cuenta.getEstado())) {
            throw conflicto("La cuenta ya está cerrada");
        }
    }

    private void exigirBorrador(CuentaLinea linea) {
        if (linea.getEstado() != EstadoLinea.BORRADOR) {
            throw conflicto("El producto ya fue enviado y no se puede modificar aquí");
        }
    }

    private void exigirMesaOperativa(Mesa mesa) {
        if (!Boolean.TRUE.equals(mesa.getActiva())) {
            throw conflicto("La mesa está inactiva");
        }
        if (Boolean.TRUE.equals(mesa.getBloqueada())) {
            throw conflicto("La mesa está bloqueada");
        }
    }

    private String describir(CuentaLinea linea) {
        return linea.getCantidad() + "x " + linea.getNombre() + " @ " + linea.getPrecioUnitario().toPlainString()
                + (linea.getNota() == null ? "" : " nota=" + linea.getNota());
    }

    private String nombres(List<Mesa> mesas) {
        return mesas.stream().map(Mesa::getNombre).collect(Collectors.joining(", "));
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