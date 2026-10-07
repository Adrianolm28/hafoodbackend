package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.dto.CategoriaDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.CategoriaRepository;
import com.hafood.sistema.service.impl.ICategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService implements ICategoriaService {

    private static final int NOMBRE_MAX = 40;

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> listarTodas(TipoCategoria tipo, Pageable pageable) {
        Page<Categoria> page = tipo == null
                ? categoriaRepository.findAll(pageable)
                : categoriaRepository.findByTipo(tipo, pageable);
        return page.map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> listarActivas(TipoCategoria tipo, Pageable pageable) {
        Page<Categoria> page = tipo == null
                ? categoriaRepository.findByActivoTrue(pageable)
                : categoriaRepository.findByActivoTrueAndTipo(tipo, pageable);
        return page.map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaDTO obtenerPorId(Long id) {
        return Mapper.toDTO(buscar(id));
    }

    @Override
    @Transactional
    public CategoriaDTO crear(CategoriaDTO dto) {
        validarDatos(dto);
        String nombre = dto.getNombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCaseAndTipo(nombre, dto.getTipo())) {
            throw new ValidationException("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .tipo(dto.getTipo())
                .build();

        return Mapper.toDTO(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaDTO actualizar(Long id, CategoriaDTO dto) {
        validarDatos(dto);
        Categoria categoria = buscar(id);
        String nombre = dto.getNombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCaseAndTipoAndIdNot(nombre, dto.getTipo(), id)) {
            throw new ValidationException("Ya existe una categoría con ese nombre");
        }

        categoria.setNombre(nombre);
        categoria.setTipo(dto.getTipo());

        if (dto.getActivo() != null) {
            categoria.setActivo(dto.getActivo());
        }

        return Mapper.toDTO(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Categoria categoria = buscar(id);
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La categoría no existe"));
    }

    private void validarDatos(CategoriaDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la categoría son obligatorios");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new ValidationException("El nombre es obligatorio");
        }
        if (dto.getNombre().trim().length() > NOMBRE_MAX) {
            throw new ValidationException("El nombre no puede superar los " + NOMBRE_MAX + " caracteres");
        }
        if (dto.getTipo() == null) {
            throw new ValidationException("El tipo de categoría es obligatorio");
        }
    }
}