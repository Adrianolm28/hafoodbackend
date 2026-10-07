package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoMovimiento;
import com.hafood.sistema.constant.UnidadIngreso;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.inventario.InsumoRendimiento;
import com.hafood.sistema.domain.inventario.Transformacion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.TransformacionDTO;
import com.hafood.sistema.dto.request.TransformacionRequest;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.InsumoRendimientoRepository;
import com.hafood.sistema.repository.TransformacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransformacionService {

    private static final int CANTIDAD_DECIMALES = 3;
    private static final int COSTO_DECIMALES = 6;

    private final TransformacionRepository transformacionRepository;
    private final InsumoRendimientoRepository rendimientoRepository;
    private final InventarioService inventarioService;
    private final CantidadConverter cantidadConverter;

    @Transactional(readOnly = true)
    public Page<TransformacionDTO> listarPorSede(Long sedeId, Pageable pageable) {
        return transformacionRepository.findByUbicacionSedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Transactional
    public TransformacionDTO registrar(TransformacionRequest request, Long usuarioId) {
        if (request.getInsumoOrigenId().equals(request.getInsumoDestinoId())) {
            throw invalido("El origen y el destino deben ser insumos distintos");
        }

        Ubicacion ubicacion = inventarioService.buscarUbicacion(request.getUbicacionId());
        Insumo origen = inventarioService.buscarInsumo(request.getInsumoOrigenId());
        Insumo destino = inventarioService.buscarInsumo(request.getInsumoDestinoId());
        Usuario usuario = inventarioService.buscarUsuario(usuarioId);

        if (!Boolean.TRUE.equals(origen.getActivo()) || !Boolean.TRUE.equals(destino.getActivo())) {
            throw invalido("Los dos insumos deben estar activos");
        }

        BigDecimal cantidadOrigen = cantidadConverter.aUnidadBase(origen, request.getCantidadOrigen(), request.getUnidadIngreso());
        boolean estimada = request.getCantidadDestino() == null;
        BigDecimal cantidadDestino;

        if (estimada) {
            InsumoRendimiento rendimiento = rendimientoRepository
                    .findByInsumoOrigenIdAndInsumoDestinoId(origen.getId(), destino.getId())
                    .orElseThrow(() -> invalido("No hay un rendimiento registrado para estos insumos. Indica la cantidad obtenida"));
            cantidadDestino = cantidadOrigen.multiply(rendimiento.getRendimiento())
                    .setScale(CANTIDAD_DECIMALES, RoundingMode.HALF_UP);
        } else {
            cantidadDestino = cantidadConverter.aUnidadBase(destino, request.getCantidadDestino(), UnidadIngreso.BASE);
        }

        if (cantidadDestino.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalido("La cantidad obtenida debe ser mayor a cero");
        }

        Transformacion guardada = transformacionRepository.save(Transformacion.builder()
                .ubicacion(ubicacion)
                .insumoOrigen(origen)
                .insumoDestino(destino)
                .cantidadOrigen(cantidadOrigen)
                .cantidadDestino(cantidadDestino)
                .estimada(estimada)
                .usuario(usuario)
                .observacion(textoONulo(request.getObservacion()))
                .fecha(LocalDateTime.now())
                .build());

        String referencia = "TRANSFORMACION-" + guardada.getId();

        inventarioService.aplicarSalida(origen, ubicacion, usuario, cantidadOrigen,
                TipoMovimiento.SALIDA_TRANSFORMACION,
                "Transformación #" + guardada.getId() + " hacia " + destino.getNombre(), referencia);
        inventarioService.aplicarEntrada(destino, ubicacion, usuario, cantidadDestino,
                TipoMovimiento.ENTRADA_TRANSFORMACION,
                "Transformación #" + guardada.getId() + " desde " + origen.getNombre(), referencia);

        if (origen.getCostoUnitario() != null && origen.getCostoUnitario().compareTo(BigDecimal.ZERO) > 0) {
            destino.setCostoUnitario(origen.getCostoUnitario()
                    .multiply(cantidadOrigen)
                    .divide(cantidadDestino, COSTO_DECIMALES, RoundingMode.HALF_UP));
        }

        return Mapper.toDTO(guardada);
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