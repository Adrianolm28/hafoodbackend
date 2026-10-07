package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoPrestamo;
import com.hafood.sistema.constant.TipoMovimiento;
import com.hafood.sistema.constant.TipoTraspaso;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.inventario.Prestamo;
import com.hafood.sistema.domain.inventario.PrestamoDetalle;
import com.hafood.sistema.domain.inventario.Traspaso;
import com.hafood.sistema.domain.inventario.TraspasoDetalle;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.PrestamoDTO;
import com.hafood.sistema.dto.request.DevolucionRequest;
import com.hafood.sistema.dto.request.TraspasoDetalleRequest;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.PrestamoRepository;
import com.hafood.sistema.repository.TraspasoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private static final Set<String> ROLES = Set.of("TODOS", "PRESTAMISTA", "PRESTATARIO");

    private final PrestamoRepository prestamoRepository;
    private final TraspasoRepository traspasoRepository;
    private final InventarioService inventarioService;
    private final CantidadConverter cantidadConverter;

    @Transactional(readOnly = true)
    public Page<PrestamoDTO> listar(Long sedeId, String rol, boolean soloPendientes, Pageable pageable) {
        String rolNormalizado = rol == null || rol.isBlank() ? "TODOS" : rol.trim().toUpperCase();

        if (!ROLES.contains(rolNormalizado)) {
            throw invalido("El filtro de rol no es válido");
        }

        return prestamoRepository.buscar(sedeId, rolNormalizado, soloPendientes, pageable).map(Mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public PrestamoDTO obtener(Long id) {
        return prestamoRepository.findById(id)
                .map(Mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El préstamo no existe"));
    }

    @Transactional
    public PrestamoDTO registrarDevolucion(Long prestamoId, DevolucionRequest request, Long usuarioId) {
        Prestamo prestamo = prestamoRepository.findForUpdate(prestamoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El préstamo no existe"));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw invalido("El préstamo ya fue devuelto por completo");
        }

        Ubicacion origen = inventarioService.buscarUbicacion(request.getOrigenId());
        Ubicacion destino = prestamo.getTraspaso().getOrigen();
        Long sedePrestataria = prestamo.getTraspaso().getDestino().getSede().getId();

        if (!origen.getSede().getId().equals(sedePrestataria)) {
            throw invalido("La devolución debe salir de la sede que recibió el préstamo");
        }

        Usuario usuario = inventarioService.buscarUsuario(usuarioId);

        Traspaso devolucion = Traspaso.builder()
                .origen(origen)
                .destino(destino)
                .usuario(usuario)
                .observacion(textoONulo(request.getObservacion()))
                .fecha(LocalDateTime.now())
                .tipo(TipoTraspaso.DEVOLUCION)
                .prestamo(prestamo)
                .build();

        Set<Long> vistos = new HashSet<>();

        for (TraspasoDetalleRequest linea : request.getDetalles()) {
            if (!vistos.add(linea.getInsumoId())) {
                throw invalido("Un insumo no puede repetirse en la misma devolución");
            }

            PrestamoDetalle detalle = prestamo.getDetalles().stream()
                    .filter(item -> item.getInsumo().getId().equals(linea.getInsumoId()))
                    .findFirst()
                    .orElseThrow(() -> invalido("El insumo no forma parte del préstamo"));

            Insumo insumo = detalle.getInsumo();
            BigDecimal cantidad = cantidadConverter.aUnidadBase(insumo, linea.getCantidad(), linea.getUnidadIngreso());
            BigDecimal pendiente = detalle.getCantidadPrestada().subtract(detalle.getCantidadDevuelta());

            if (cantidad.compareTo(pendiente) > 0) {
                throw invalido("Solo quedan " + pendiente.stripTrailingZeros().toPlainString()
                        + " pendientes de " + insumo.getNombre());
            }

            devolucion.getDetalles().add(TraspasoDetalle.builder()
                    .traspaso(devolucion)
                    .insumo(insumo)
                    .cantidad(cantidad)
                    .build());

            detalle.setCantidadDevuelta(detalle.getCantidadDevuelta().add(cantidad));
        }

        Traspaso guardada = traspasoRepository.save(devolucion);
        String referencia = "TRASPASO-" + guardada.getId();
        String motivo = "Devolución del préstamo #" + prestamo.getId() + " (traspaso #" + guardada.getId() + ")";

        for (TraspasoDetalle detalle : guardada.getDetalles()) {
            inventarioService.aplicarSalida(detalle.getInsumo(), origen, usuario, detalle.getCantidad(),
                    TipoMovimiento.SALIDA_TRASPASO, motivo, referencia);
            inventarioService.aplicarEntrada(detalle.getInsumo(), destino, usuario, detalle.getCantidad(),
                    TipoMovimiento.ENTRADA_TRASPASO, motivo, referencia);
        }

        prestamo.setEstado(calcularEstado(prestamo));
        return Mapper.toDTO(prestamo);
    }

    private EstadoPrestamo calcularEstado(Prestamo prestamo) {
        boolean todoDevuelto = prestamo.getDetalles().stream()
                .allMatch(item -> item.getCantidadDevuelta().compareTo(item.getCantidadPrestada()) >= 0);

        if (todoDevuelto) {
            return EstadoPrestamo.DEVUELTO;
        }

        boolean algoDevuelto = prestamo.getDetalles().stream()
                .anyMatch(item -> item.getCantidadDevuelta().compareTo(BigDecimal.ZERO) > 0);

        return algoDevuelto ? EstadoPrestamo.PARCIAL : EstadoPrestamo.PENDIENTE;
    }

    private String textoONulo(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}