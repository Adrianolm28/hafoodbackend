package com.hafood.sistema.service;

import com.hafood.sistema.domain.cocina.Plato;
import com.hafood.sistema.domain.cocina.RecetaPlato;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.dto.RecetaPlatoDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.InsumoRepository;
import com.hafood.sistema.repository.PlatoRepository;
import com.hafood.sistema.repository.cocina.RecetaPlatoRepository;
import com.hafood.sistema.service.impl.IRecetaPlatoService;
import com.hafood.sistema.util.RecetaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaPlatoService implements IRecetaPlatoService {

    private final RecetaPlatoRepository recetaPlatoRepository;
    private final PlatoRepository platoRepository;
    private final InsumoRepository insumoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RecetaPlatoDTO> listarTodos(Pageable pageable) {
        return recetaPlatoRepository.findAll(pageable)
                .map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecetaPlatoDTO> listarPorPlato(Long platoId, Pageable pageable) {
        if (!platoRepository.existsById(platoId)) {
            throw new NotFoundException("El plato no existe");
        }
        return recetaPlatoRepository.findByPlatoId(platoId, pageable)
                .map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public RecetaPlatoDTO obtenerPorId(Long id) {
        RecetaPlato receta = recetaPlatoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El detalle de la receta no existe"));
        return Mapper.toDTO(receta);
    }

    @Override
    @Transactional
    public RecetaPlatoDTO agregarInsumo(RecetaPlatoDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la receta son obligatorios");
        }
        if (dto.getPlatoId() == null) {
            throw new ValidationException("El plato es obligatorio");
        }
        if (dto.getInsumoId() == null) {
            throw new ValidationException("El insumo es obligatorio");
        }
        RecetaValidator.validarCantidad(dto.getCantidad());

        Plato plato = platoRepository.findById(dto.getPlatoId())
                .orElseThrow(() -> new NotFoundException("El plato no existe"));

        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new NotFoundException("El insumo no existe"));

        RecetaValidator.validarUnidad(dto.getUnidadMedida(), insumo);

        if (recetaPlatoRepository.existsByPlatoIdAndInsumoId(dto.getPlatoId(), dto.getInsumoId())) {
            throw new ValidationException("El insumo ya está registrado en la receta de este plato");
        }

        RecetaPlato receta = RecetaPlato.builder()
                .plato(plato)
                .insumo(insumo)
                .cantidad(dto.getCantidad())
                .unidadMedida(insumo.getUnidadMedida())
                .build();

        return Mapper.toDTO(recetaPlatoRepository.save(receta));
    }

    @Override
    @Transactional
    public RecetaPlatoDTO actualizar(Long id, RecetaPlatoDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la receta son obligatorios");
        }

        RecetaPlato receta = recetaPlatoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El detalle de la receta no existe"));

        RecetaValidator.validarCantidad(dto.getCantidad());
        RecetaValidator.validarUnidad(dto.getUnidadMedida(), receta.getInsumo());


        receta.setCantidad(dto.getCantidad());
        receta.setUnidadMedida(receta.getInsumo().getUnidadMedida());

        return Mapper.toDTO(recetaPlatoRepository.save(receta));
    }

    @Override
    @Transactional
    public void eliminarInsumo(Long id) {
        if (!recetaPlatoRepository.existsById(id)) {
            throw new NotFoundException("El detalle de la receta no existe");
        }
        recetaPlatoRepository.deleteById(id);
    }
}