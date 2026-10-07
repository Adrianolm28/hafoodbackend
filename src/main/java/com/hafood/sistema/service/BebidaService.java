package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.domain.barra.Bebida;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.dto.BebidaDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.BebidaRepository;
import com.hafood.sistema.repository.CategoriaRepository;
import com.hafood.sistema.service.impl.IBebidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BebidaService implements IBebidaService {

    private static final int NOMBRE_MAX = 40;
    private static final int DESCRIPCION_MAX = 120;

    private final BebidaRepository bebidaRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BebidaDTO> listarTodos(Pageable pageable) {
        return bebidaRepository.findAll(pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BebidaDTO> listarActivos(Pageable pageable) {
        return bebidaRepository.findAllByActivo(true, pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public BebidaDTO obtenerPorId(Long id) {
        return Mapper.toDTO(buscar(id));
    }

    @Override
    @Transactional
    public BebidaDTO crear(BebidaDTO dto) {
        validar(dto);

        Bebida bebida = Bebida.builder()
                .nombre(dto.getNombre().trim())
                .descripcion(textoONulo(dto.getDescripcion()))
                .precioVenta(dto.getPrecioVenta())
                .categoria(resolverCategoria(dto.getCategoriaId()))
                .build();

        return Mapper.toDTO(bebidaRepository.save(bebida));
    }

    @Override
    @Transactional
    public BebidaDTO actualizar(Long id, BebidaDTO dto) {
        validar(dto);
        Bebida bebida = buscar(id);

        bebida.setNombre(dto.getNombre().trim());
        bebida.setDescripcion(textoONulo(dto.getDescripcion()));
        bebida.setPrecioVenta(dto.getPrecioVenta());
        bebida.setCategoria(resolverCategoria(dto.getCategoriaId()));

        if (dto.getActivo() != null) {
            bebida.setActivo(dto.getActivo());
        }

        return Mapper.toDTO(bebidaRepository.save(bebida));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Bebida bebida = buscar(id);
        bebida.setActivo(false);
        bebidaRepository.save(bebida);
    }

    private Bebida buscar(Long id) {
        return bebidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La bebida no existe"));
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
        if (categoria.getTipo() != TipoCategoria.BEBIDA) {
            throw new ValidationException("La categoría no corresponde a bebidas");
        }
        return categoria;
    }

    private void validar(BebidaDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la bebida son obligatorios");
        }
        String nombre = textoONulo(dto.getNombre());
        if (nombre == null) {
            throw new ValidationException("El nombre de la bebida es obligatorio");
        }
        if (nombre.length() > NOMBRE_MAX) {
            throw new ValidationException("El nombre no puede superar los " + NOMBRE_MAX + " caracteres");
        }
        String descripcion = textoONulo(dto.getDescripcion());
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX) {
            throw new ValidationException("La descripción no puede superar los " + DESCRIPCION_MAX + " caracteres");
        }
        if (dto.getPrecioVenta() == null || dto.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El precio de venta debe ser mayor a cero");
        }
    }

    private String textoONulo(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}