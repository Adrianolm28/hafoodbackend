package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoMovimiento;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.inventario.MovimientoInsumo;
import com.hafood.sistema.domain.inventario.StockUbicacion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.MovimientoInsumoDTO;
import com.hafood.sistema.dto.StockUbicacionDTO;
import com.hafood.sistema.dto.request.MovimientoRequest;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.InsumoRepository;
import com.hafood.sistema.repository.MovimientoInsumoRepository;
import com.hafood.sistema.repository.StockUbicacionRepository;
import com.hafood.sistema.repository.UbicacionRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private static final Set<TipoMovimiento> ENTRADAS_MANUALES =
            EnumSet.of(TipoMovimiento.ENTRADA_COMPRA, TipoMovimiento.ENTRADA_AJUSTE);
    private static final Set<TipoMovimiento> SALIDAS_MANUALES =
            EnumSet.of(TipoMovimiento.SALIDA_MERMA, TipoMovimiento.SALIDA_AJUSTE);

    private final StockUbicacionRepository stockUbicacionRepository;
    private final MovimientoInsumoRepository movimientoInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CantidadConverter cantidadConverter;

    @Transactional(readOnly = true)
    public Page<StockUbicacionDTO> stockPorSede(Long sedeId, Pageable pageable) {
        return stockUbicacionRepository.findByUbicacionSedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<StockUbicacionDTO> stockPorUbicacion(Long ubicacionId, Pageable pageable) {
        return stockUbicacionRepository.findByUbicacionId(ubicacionId, pageable).map(Mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInsumoDTO> kardexPorSede(Long sedeId, Pageable pageable) {
        return movimientoInsumoRepository.findBySedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInsumoDTO> kardexPorUbicacion(Long ubicacionId, Pageable pageable) {
        return movimientoInsumoRepository.findByUbicacionId(ubicacionId, pageable).map(Mapper::toDTO);
    }

    @Transactional
    public MovimientoInsumoDTO registrarEntrada(MovimientoRequest request, Long usuarioId) {
        if (!ENTRADAS_MANUALES.contains(request.getTipoMovimiento())) {
            throw invalido("El tipo de entrada no está permitido");
        }

        Insumo insumo = buscarInsumo(request.getInsumoId());
        Ubicacion ubicacion = buscarUbicacion(request.getUbicacionId());
        Usuario usuario = buscarUsuario(usuarioId);
        BigDecimal cantidad = cantidadConverter.aUnidadBase(insumo, request.getCantidad(), request.getUnidadIngreso());

        return Mapper.toDTO(aplicarEntrada(insumo, ubicacion, usuario, cantidad,
                request.getTipoMovimiento(), request.getMotivo(), null));
    }

    @Transactional
    public MovimientoInsumoDTO registrarSalida(MovimientoRequest request, Long usuarioId) {
        if (!SALIDAS_MANUALES.contains(request.getTipoMovimiento())) {
            throw invalido("El tipo de salida no está permitido");
        }

        Insumo insumo = buscarInsumo(request.getInsumoId());
        Ubicacion ubicacion = buscarUbicacion(request.getUbicacionId());
        Usuario usuario = buscarUsuario(usuarioId);
        BigDecimal cantidad = cantidadConverter.aUnidadBase(insumo, request.getCantidad(), request.getUnidadIngreso());

        return Mapper.toDTO(aplicarSalida(insumo, ubicacion, usuario, cantidad,
                request.getTipoMovimiento(), request.getMotivo(), null));
    }

    @Transactional
    public MovimientoInsumo aplicarEntrada(Insumo insumo, Ubicacion ubicacion, Usuario usuario,
                                           BigDecimal cantidad, TipoMovimiento tipo,
                                           String motivo, String referencia) {
        if (!Boolean.TRUE.equals(insumo.getActivo())) {
            throw invalido("El insumo " + insumo.getNombre() + " está desactivado");
        }

        StockUbicacion stock = stockUbicacionRepository.findForUpdate(insumo.getId(), ubicacion.getId())
                .orElseGet(() -> StockUbicacion.builder()
                        .insumo(insumo)
                        .ubicacion(ubicacion)
                        .cantidadActual(BigDecimal.ZERO)
                        .build());

        BigDecimal nuevoStock = stock.getCantidadActual().add(cantidad);
        stock.setCantidadActual(nuevoStock);
        stockUbicacionRepository.save(stock);

        return guardarMovimiento(insumo, ubicacion, usuario, tipo, cantidad, nuevoStock, motivo, referencia);
    }

    @Transactional
    public MovimientoInsumo aplicarSalida(Insumo insumo, Ubicacion ubicacion, Usuario usuario,
                                          BigDecimal cantidad, TipoMovimiento tipo,
                                          String motivo, String referencia) {
        StockUbicacion stock = stockUbicacionRepository.findForUpdate(insumo.getId(), ubicacion.getId())
                .orElseThrow(() -> invalido("No hay stock de " + insumo.getNombre() + " en " + ubicacion.getNombre()));

        if (stock.getCantidadActual().compareTo(cantidad) < 0) {
            throw invalido("Stock insuficiente de " + insumo.getNombre() + ". Disponible: "
                    + stock.getCantidadActual().stripTrailingZeros().toPlainString());
        }

        BigDecimal nuevoStock = stock.getCantidadActual().subtract(cantidad);
        stock.setCantidadActual(nuevoStock);
        stockUbicacionRepository.save(stock);

        return guardarMovimiento(insumo, ubicacion, usuario, tipo, cantidad, nuevoStock, motivo, referencia);
    }

    public Insumo buscarInsumo(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El insumo no existe"));
    }

    public Ubicacion buscarUbicacion(Long id) {
        return ubicacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La ubicación no existe"));
    }

    public Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
    }

    private MovimientoInsumo guardarMovimiento(Insumo insumo, Ubicacion ubicacion, Usuario usuario,
                                               TipoMovimiento tipo, BigDecimal cantidad,
                                               BigDecimal stockResultante, String motivo, String referencia) {
        MovimientoInsumo movimiento = MovimientoInsumo.builder()
                .insumo(insumo)
                .sede(ubicacion.getSede())
                .ubicacion(ubicacion)
                .usuario(usuario)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .stockResultante(stockResultante)
                .motivo(motivo)
                .referencia(referencia)
                .fechaMovimiento(LocalDateTime.now())
                .build();

        return movimientoInsumoRepository.save(movimiento);
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public record ResultadoSalida(MovimientoInsumo movimiento, BigDecimal faltante) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoSalida salidaPermitiendoFaltante(Insumo insumo, Ubicacion ubicacion, Usuario usuario,
                                                     BigDecimal cantidad, TipoMovimiento tipo,
                                                     String motivo, String referencia) {
        StockUbicacion stock = bloquearOCrear(insumo, ubicacion);
        BigDecimal anterior = stock.getCantidadActual();
        BigDecimal faltante = cantidad.subtract(anterior.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
        BigDecimal nuevoStock = anterior.subtract(cantidad);

        stock.setCantidadActual(nuevoStock);
        stockUbicacionRepository.save(stock);

        MovimientoInsumo movimiento =
                guardarMovimiento(insumo, ubicacion, usuario, tipo, cantidad, nuevoStock, motivo, referencia);

        return new ResultadoSalida(movimiento, faltante);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoInsumo entradaSinValidar(Insumo insumo, Ubicacion ubicacion, Usuario usuario,
                                              BigDecimal cantidad, TipoMovimiento tipo,
                                              String motivo, String referencia) {
        StockUbicacion stock = bloquearOCrear(insumo, ubicacion);
        BigDecimal nuevoStock = stock.getCantidadActual().add(cantidad);

        stock.setCantidadActual(nuevoStock);
        stockUbicacionRepository.save(stock);

        return guardarMovimiento(insumo, ubicacion, usuario, tipo, cantidad, nuevoStock, motivo, referencia);
    }

    private StockUbicacion bloquearOCrear(Insumo insumo, Ubicacion ubicacion) {
        stockUbicacionRepository.crearSiNoExiste(insumo.getId(), ubicacion.getId());

        return stockUbicacionRepository.findForUpdate(insumo.getId(), ubicacion.getId())
                .orElseThrow(() -> invalido("No se pudo preparar el stock de " + insumo.getNombre()
                        + " en " + ubicacion.getNombre()));
    }
}