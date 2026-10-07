package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoMovimiento;
import com.hafood.sistema.constant.TipoUbicacion;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Compra;
import com.hafood.sistema.domain.inventario.CompraDetalle;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CompraDTO;
import com.hafood.sistema.dto.request.CompraDetalleRequest;
import com.hafood.sistema.dto.request.CompraRequest;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hafood.sistema.domain.inventario.Proveedor;
import com.hafood.sistema.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final int COSTO_DECIMALES = 6;

    private final CompraRepository compraRepository;
    private final InventarioService inventarioService;
    private final CantidadConverter cantidadConverter;
    private final ProveedorRepository proveedorRepository;
    @Transactional(readOnly = true)
    public Page<CompraDTO> listarPorSede(Long sedeId, Pageable pageable) {
        return compraRepository.findByUbicacionSedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(Long id) {
        return compraRepository.findById(id)
                .map(Mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La compra no existe"));
    }

    @Transactional
    public CompraDTO registrar(CompraRequest request, Long usuarioId) {
        Ubicacion ubicacion = inventarioService.buscarUbicacion(request.getUbicacionId());
        if (ubicacion.getTipo() != TipoUbicacion.ALMACEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las compras se registran en el almacén de la sede");
        }

        Usuario registrador = inventarioService.buscarUsuario(usuarioId);
        Usuario firmante = inventarioService.buscarUsuario(request.getFirmanteId());

        Proveedor proveedor = null;
        if (request.getProveedorId() != null) {
            proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El proveedor no existe"));
            if (!Boolean.TRUE.equals(proveedor.getActivo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor está desactivado");
            }
        }

        Compra compra = Compra.builder()
                .ubicacion(ubicacion)
                .fechaCompra(request.getFechaCompra())
                .compradoPor(request.getCompradoPor().trim())
                .firmante(firmante)
                .registradoPor(registrador)
                .proveedor(proveedor)
                .observacion(textoONulo(request.getObservacion()))
                .totalPagado(BigDecimal.ZERO)
                .fechaRegistro(LocalDateTime.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> vistos = new HashSet<>();

        for (CompraDetalleRequest linea : request.getDetalles()) {
            if (!vistos.add(linea.getInsumoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un insumo no puede repetirse en la misma compra");
            }

            Insumo insumo = inventarioService.buscarInsumo(linea.getInsumoId());
            BigDecimal recibida = cantidadConverter.aUnidadBase(insumo, linea.getCantidad(), linea.getUnidadIngreso());
            BigDecimal esperada = linea.getCantidadEsperada() == null
                    ? null
                    : cantidadConverter.aUnidadBase(insumo, linea.getCantidadEsperada(), linea.getUnidadIngreso());
            BigDecimal costoUnitario = linea.getMontoPagado().divide(recibida, COSTO_DECIMALES, RoundingMode.HALF_UP);

            compra.getDetalles().add(CompraDetalle.builder()
                    .compra(compra)
                    .insumo(insumo)
                    .cantidadIngresada(linea.getCantidad())
                    .unidadIngreso(linea.getUnidadIngreso() == null
                            ? com.hafood.sistema.constant.UnidadIngreso.BASE
                            : linea.getUnidadIngreso())
                    .cantidadRecibida(recibida)
                    .cantidadEsperada(esperada)
                    .montoPagado(linea.getMontoPagado())
                    .costoUnitario(costoUnitario)
                    .build());

            total = total.add(linea.getMontoPagado());
        }

        compra.setTotalPagado(total);
        Compra guardada = compraRepository.save(compra);
        String referencia = "COMPRA-" + guardada.getId();

        for (CompraDetalle detalle : guardada.getDetalles()) {
            inventarioService.aplicarEntrada(detalle.getInsumo(), ubicacion, registrador,
                    detalle.getCantidadRecibida(), TipoMovimiento.ENTRADA_COMPRA,
                    "Compra #" + guardada.getId(), referencia);
            detalle.getInsumo().setCostoUnitario(detalle.getCostoUnitario());
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
}