package com.hafood.sistema.service;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.domain.barra.Bebida;
import com.hafood.sistema.domain.barra.RecetaBebida;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.dto.RecetaBebidaDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.BebidaRepository;
import com.hafood.sistema.repository.InsumoRepository;
import com.hafood.sistema.repository.barra.RecetaBebidaRepository;
import com.hafood.sistema.service.impl.IRecetaBebidaService;
import com.hafood.sistema.util.RecetaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaBebidaService implements IRecetaBebidaService {

    private final RecetaBebidaRepository recetaBebidaRepository;
    private final BebidaRepository bebidaRepository;
    private final InsumoRepository insumoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RecetaBebidaDTO> listarTodos(Pageable pageable) {
        return recetaBebidaRepository.findAll(pageable)
                .map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecetaBebidaDTO> listarPorBebida(Long bebidaId, Pageable pageable) {
        if (!bebidaRepository.existsById(bebidaId)) {
            throw new NotFoundException("La bebida no existe");
        }
        return recetaBebidaRepository.findByBebidaId(bebidaId, pageable)
                .map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public RecetaBebidaDTO obtenerPorId(Long id) {
        RecetaBebida receta = recetaBebidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El detalle de la receta no existe"));
        return Mapper.toDTO(receta);
    }

    @Override
    @Transactional
    public RecetaBebidaDTO agregarInsumo(RecetaBebidaDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la receta son obligatorios");
        }
        if (dto.getBebidaId() == null) {
            throw new ValidationException("La bebida es obligatoria");
        }
        if (dto.getInsumoId() == null) {
            throw new ValidationException("El insumo es obligatorio");
        }
        RecetaValidator.validarCantidad(dto.getCantidad());

        Bebida bebida = bebidaRepository.findById(dto.getBebidaId())
                .orElseThrow(() -> new NotFoundException("La bebida no existe"));

        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new NotFoundException("El insumo no existe"));

        RecetaValidator.validarUnidad(dto.getUnidadMedida(), insumo);
        RecetaValidator.validarArea(insumo, AreaInsumo.BARRA);

        if (recetaBebidaRepository.existsByBebidaIdAndInsumoId(dto.getBebidaId(), dto.getInsumoId())) {
            throw new ValidationException("El insumo ya está registrado en la receta de esta bebida");
        }

        RecetaBebida receta = RecetaBebida.builder()
                .bebida(bebida)
                .insumo(insumo)
                .cantidad(dto.getCantidad())
                .unidadMedida(insumo.getUnidadMedida())
                .build();

        return Mapper.toDTO(recetaBebidaRepository.save(receta));
    }

    @Override
    @Transactional
    public RecetaBebidaDTO actualizar(Long id, RecetaBebidaDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la receta son obligatorios");
        }

        RecetaBebida receta = recetaBebidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El detalle de la receta no existe"));

        RecetaValidator.validarCantidad(dto.getCantidad());
        RecetaValidator.validarUnidad(dto.getUnidadMedida(), receta.getInsumo());

        receta.setCantidad(dto.getCantidad());
        receta.setUnidadMedida(receta.getInsumo().getUnidadMedida());

        return Mapper.toDTO(recetaBebidaRepository.save(receta));
    }

    @Override
    @Transactional
    public void eliminarInsumo(Long id) {
        if (!recetaBebidaRepository.existsById(id)) {
            throw new NotFoundException("El detalle de la receta no existe");
        }
        recetaBebidaRepository.deleteById(id);
    }
}