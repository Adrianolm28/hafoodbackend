package com.hafood.sistema.service;

import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.dto.SedeDTO;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.SeccionRepository;
import com.hafood.sistema.repository.SedeRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import com.hafood.sistema.service.impl.ISedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SedeService implements ISedeService {

    private final SedeRepository sedeRepository;
    private final SeccionRepository seccionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SedeDTO> listar(Pageable pageable) {
        return sedeRepository.findAll(pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SedeDTO obtenerPorId(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));
        return Mapper.toDTO(sede);
    }

    @Override
    @Transactional
    public SedeDTO crear(SedeDTO dto) {
        validarSedeDTO(dto);
        Sede sede = Sede.builder()
                .nombre(dto.getNombre().trim())
                .direccion(dto.getDireccion() != null ? dto.getDireccion().trim() : null)
                .build();
        return Mapper.toDTO(sedeRepository.save(sede));
    }

    @Override
    @Transactional
    public SedeDTO actualizar(Long id, SedeDTO dto) {
        validarSedeDTO(dto);
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));

        sede.setNombre(dto.getNombre().trim());
        sede.setDireccion(dto.getDireccion() != null ? dto.getDireccion().trim() : null);

        return Mapper.toDTO(sedeRepository.save(sede));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!sedeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada");
        }

        boolean tieneSecciones = seccionRepository.existsBySedeId(id);
        boolean tieneUsuarios = usuarioRepository.existsBySedeId(id);

        if (tieneSecciones || tieneUsuarios) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar la sede porque tiene secciones o usuarios asociados"
            );
        }

        sedeRepository.deleteById(id);
    }

    private void validarSedeDTO(SedeDTO dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la sede es obligatorio");
        }
    }
}