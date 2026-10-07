package com.hafood.sistema.service;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.dto.InsumoDTO;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.InsumoRepository;
import com.hafood.sistema.repository.barra.RecetaBebidaRepository;
import com.hafood.sistema.repository.cocina.RecetaPlatoRepository;
import com.hafood.sistema.service.impl.IInsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsumoService implements IInsumoService {

    private static final int NOMBRE_MAX = 60;
    private static final int PRESENTACION_NOMBRE_MAX = 30;
    private static final int COSTO_MAX_DECIMALES = 6;
    private static final int PRESENTACION_MAX_DECIMALES = 3;

    private final InsumoRepository insumoRepository;
    private final RecetaPlatoRepository recetaPlatoRepository;
    private final RecetaBebidaRepository recetaBebidaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InsumoDTO> listarTodos(AreaInsumo area, Pageable pageable) {
        Page<Insumo> page = area == null
                ? insumoRepository.findAll(pageable)
                : insumoRepository.findByAreas(areasVisibles(area), pageable);
        return page.map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InsumoDTO> listarActivos(AreaInsumo area, Pageable pageable) {
        Page<Insumo> page = area == null
                ? insumoRepository.findAllByActivo(true, pageable)
                : insumoRepository.findActivosByAreas(areasVisibles(area), pageable);
        return page.map(Mapper::toDTO);
    }

    private List<AreaInsumo> areasVisibles(AreaInsumo area) {
        return area == AreaInsumo.AMBAS ? List.of(AreaInsumo.AMBAS) : List.of(area, AreaInsumo.AMBAS);
    }

    @Override
    @Transactional(readOnly = true)
    public InsumoDTO obtenerPorId(Long id) {
        return Mapper.toDTO(buscar(id));
    }

    @Override
    @Transactional
    public InsumoDTO crear(InsumoDTO dto) {
        validarInsumo(dto);

        Insumo insumo = Insumo.builder().activo(true).build();
        aplicarDatos(insumo, dto);

        return Mapper.toDTO(insumoRepository.save(insumo));
    }

    @Override
    @Transactional
    public InsumoDTO actualizar(Long id, InsumoDTO dto) {
        validarInsumo(dto);

        Insumo insumo = buscar(id);

        if (insumo.getUnidadBase() != dto.getUnidadBase() && estaEnRecetas(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cambiar la unidad base porque el insumo ya se usa en recetas"
            );
        }

        aplicarDatos(insumo, dto);

        if (dto.getActivo() != null) {
            insumo.setActivo(dto.getActivo());
        }

        return Mapper.toDTO(insumoRepository.save(insumo));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Insumo insumo = buscar(id);
        insumo.setActivo(false);
        insumoRepository.save(insumo);
    }

    private Insumo buscar(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado"));
    }

    private boolean estaEnRecetas(Long insumoId) {
        return recetaPlatoRepository.existsByInsumoId(insumoId)
                || recetaBebidaRepository.existsByInsumoId(insumoId);
    }

    private void aplicarDatos(Insumo insumo, InsumoDTO dto) {
        String presentacionNombre = textoONulo(dto.getPresentacionNombre());

        insumo.setNombre(dto.getNombre().trim());
        insumo.setUnidadBase(dto.getUnidadBase());
        insumo.setUnidadMedida(dto.getUnidadBase().getEtiqueta());
        insumo.setPresentacionNombre(presentacionNombre);
        insumo.setPresentacionCantidad(presentacionNombre == null ? null : dto.getPresentacionCantidad());
        insumo.setCostoUnitario(dto.getCostoUnitario());
        insumo.setArea(dto.getArea() == null ? AreaInsumo.AMBAS : dto.getArea());

        if (dto.getControlEstricto() != null) {
            insumo.setControlEstricto(dto.getControlEstricto());
        }
    }

    private void validarInsumo(InsumoDTO dto) {
        if (dto == null) {
            throw invalido("Los datos del insumo son obligatorios");
        }

        String nombre = textoONulo(dto.getNombre());
        if (nombre == null) {
            throw invalido("El nombre del insumo no puede estar vacío");
        }
        if (nombre.length() > NOMBRE_MAX) {
            throw invalido("El nombre del insumo no puede superar los " + NOMBRE_MAX + " caracteres");
        }

        if (dto.getUnidadBase() == null) {
            throw invalido("La unidad base es obligatoria (ml, g o unidad)");
        }

        validarCosto(dto.getCostoUnitario());
        validarPresentacion(dto);
    }

    private void validarCosto(BigDecimal costo) {
        if (costo == null) {
            throw invalido("El costo unitario es obligatorio");
        }
        if (costo.compareTo(BigDecimal.ZERO) < 0) {
            throw invalido("El costo unitario no puede ser negativo");
        }
        if (costo.stripTrailingZeros().scale() > COSTO_MAX_DECIMALES) {
            throw invalido("El costo unitario admite hasta " + COSTO_MAX_DECIMALES + " decimales");
        }
    }

    private void validarPresentacion(InsumoDTO dto) {
        String nombre = textoONulo(dto.getPresentacionNombre());
        BigDecimal cantidad = dto.getPresentacionCantidad();

        if (nombre == null && cantidad == null) {
            return;
        }
        if (nombre == null || cantidad == null) {
            throw invalido("La presentación necesita nombre y cantidad");
        }
        if (nombre.length() > PRESENTACION_NOMBRE_MAX) {
            throw invalido("El nombre de la presentación no puede superar los " + PRESENTACION_NOMBRE_MAX + " caracteres");
        }
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalido("La cantidad de la presentación debe ser mayor a cero");
        }
        if (cantidad.stripTrailingZeros().scale() > PRESENTACION_MAX_DECIMALES) {
            throw invalido("La cantidad de la presentación admite hasta " + PRESENTACION_MAX_DECIMALES + " decimales");
        }
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