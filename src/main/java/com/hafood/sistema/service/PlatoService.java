package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.domain.cocina.Plato;
import com.hafood.sistema.dto.PlatoDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.CategoriaRepository;
import com.hafood.sistema.repository.PlatoRepository;
import com.hafood.sistema.service.impl.IPlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlatoService implements IPlatoService {

    private final PlatoRepository platoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PlatoDTO> listarTodos(Pageable pageable) {
        return platoRepository.findAll(pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlatoDTO> listarActivos(Pageable pageable) {
        return platoRepository.findAllByActivo(true, pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatoDTO obtenerPorId(Long id) {
        Plato plato = platoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado con ID: " + id));
        return Mapper.toDTO(plato);
    }

    @Override
    @Transactional
    public PlatoDTO crear(PlatoDTO dto) {
        validarReglasNegocio(dto.getNombre(), dto.getPrecioVenta());

        Plato plato = Plato.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precioVenta(dto.getPrecioVenta())
                .categoria(resolverCategoria(dto.getCategoriaId()))
                .activo(true)
                .build();

        return Mapper.toDTO(platoRepository.save(plato));
    }

    @Override
    @Transactional
    public PlatoDTO actualizar(Long id, PlatoDTO dto) {
        validarReglasNegocio(dto.getNombre(), dto.getPrecioVenta());

        Plato plato = platoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado con ID: " + id));

        plato.setNombre(dto.getNombre());
        plato.setDescripcion(dto.getDescripcion());
        plato.setPrecioVenta(dto.getPrecioVenta());
        plato.setCategoria(resolverCategoria(dto.getCategoriaId()));
        if (dto.getActivo() != null) {
            plato.setActivo(dto.getActivo());
        }

        return Mapper.toDTO(platoRepository.save(plato));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Plato plato = platoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado con ID: " + id));

        plato.setActivo(false);
        platoRepository.save(plato);
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new NotFoundException("La categoría no existe"));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ValidationException("La categoría está inactiva");
        }
        if (categoria.getTipo() != TipoCategoria.PLATO) {
            throw new ValidationException("La categoría no corresponde a platos");
        }
        return categoria;
    }

    private void validarReglasNegocio(String nombre, BigDecimal precioVenta) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del plato es obligatorio");
        }
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a cero");
        }
    }
}