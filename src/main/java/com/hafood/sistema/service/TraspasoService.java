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
import com.hafood.sistema.dto.TraspasoDTO;
import com.hafood.sistema.dto.request.TraspasoDetalleRequest;
import com.hafood.sistema.dto.request.TraspasoRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TraspasoService {

    private final TraspasoRepository traspasoRepository;
    private final PrestamoRepository prestamoRepository;
    private final InventarioService inventarioService;
    private final CantidadConverter cantidadConverter;

    @Transactional(readOnly = true)
    public Page<TraspasoDTO> listarPorSede(Long sedeId, Pageable pageable) {
        return traspasoRepository.findBySedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Transactional
    public TraspasoDTO registrar(TraspasoRequest request, Long usuarioId) {
        if (request.getOrigenId().equals(request.getDestinoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El origen y el destino deben ser distintos");
        }

        Ubicacion origen = inventarioService.buscarUbicacion(request.getOrigenId());
        Ubicacion destino = inventarioService.buscarUbicacion(request.getDestinoId());
        Usuario usuario = inventarioService.buscarUsuario(usuarioId);
        boolean esPrestamo = Boolean.TRUE.equals(request.getEsPrestamo());

        if (esPrestamo && origen.getSede().getId().equals(destino.getSede().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un préstamo debe ser entre sedes distintas");
        }

        Traspaso traspaso = Traspaso.builder()
                .origen(origen)
                .destino(destino)
                .usuario(usuario)
                .observacion(textoONulo(request.getObservacion()))
                .fecha(LocalDateTime.now())
                .tipo(esPrestamo ? TipoTraspaso.PRESTAMO : TipoTraspaso.NORMAL)
                .build();

        Set<Long> vistos = new HashSet<>();

        for (TraspasoDetalleRequest linea : request.getDetalles()) {
            if (!vistos.add(linea.getInsumoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un insumo no puede repetirse en el mismo traspaso");
            }

            Insumo insumo = inventarioService.buscarInsumo(linea.getInsumoId());
            BigDecimal cantidad = cantidadConverter.aUnidadBase(insumo, linea.getCantidad(), linea.getUnidadIngreso());

            traspaso.getDetalles().add(TraspasoDetalle.builder()
                    .traspaso(traspaso)
                    .insumo(insumo)
                    .cantidad(cantidad)
                    .build());
        }

        Traspaso guardado = traspasoRepository.save(traspaso);
        String referencia = "TRASPASO-" + guardado.getId();
        String prefijo = esPrestamo ? "Préstamo (traspaso #" + guardado.getId() + ")" : "Traspaso #" + guardado.getId();
        String motivoSalida = prefijo + " hacia " + etiqueta(destino);
        String motivoEntrada = prefijo + " desde " + etiqueta(origen);

        for (TraspasoDetalle detalle : guardado.getDetalles()) {
            inventarioService.aplicarSalida(detalle.getInsumo(), origen, usuario, detalle.getCantidad(),
                    TipoMovimiento.SALIDA_TRASPASO, motivoSalida, referencia);
            inventarioService.aplicarEntrada(detalle.getInsumo(), destino, usuario, detalle.getCantidad(),
                    TipoMovimiento.ENTRADA_TRASPASO, motivoEntrada, referencia);
        }

        if (esPrestamo) {
            crearPrestamo(guardado, request.getFechaLimite());
        }

        return Mapper.toDTO(guardado);
    }

    private void crearPrestamo(Traspaso traspaso, LocalDate fechaLimite) {
        Prestamo prestamo = Prestamo.builder()
                .traspaso(traspaso)
                .estado(EstadoPrestamo.PENDIENTE)
                .fechaLimite(fechaLimite)
                .fecha(traspaso.getFecha())
                .build();

        for (TraspasoDetalle detalle : traspaso.getDetalles()) {
            prestamo.getDetalles().add(PrestamoDetalle.builder()
                    .prestamo(prestamo)
                    .insumo(detalle.getInsumo())
                    .cantidadPrestada(detalle.getCantidad())
                    .cantidadDevuelta(BigDecimal.ZERO)
                    .build());
        }

        prestamoRepository.save(prestamo);
    }

    private String etiqueta(Ubicacion ubicacion) {
        return ubicacion.getSede().getNombre() + " - " + ubicacion.getNombre();
    }

    private String textoONulo(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}