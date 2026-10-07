package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoPersonal;
import com.hafood.sistema.domain.estructura.Personal;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.dto.SeccionDTO;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.*;
import com.hafood.sistema.service.impl.ISeccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SeccionService implements ISeccionService {

    private final SeccionRepository seccionRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final MesaRepository mesaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SeccionDTO> listar(Pageable pageable) {
        return seccionRepository.findAll(pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeccionDTO> listarPorSede(Long sedeId, Pageable pageable) {
        if (!sedeRepository.existsById(sedeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada");
        }
        return seccionRepository.findBySedeId(sedeId, pageable).map(Mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SeccionDTO obtenerPorId(Long id) {
        Seccion seccion = seccionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        return Mapper.toDTO(seccion);
    }

    @Override
    @Transactional
    public SeccionDTO crear(SeccionDTO dto) {
        validarSeccionDTO(dto);
        Sede sede = sedeRepository.findById(dto.getSedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede especificada no existe"));

        Seccion seccion = Seccion.builder()
                .nombre(dto.getNombre().trim())
                .sede(sede)
                .jefeMozo(resolverJefe(dto.getJefeMozoId(), sede, TipoPersonal.MOZO, null))
                .jefeBartender(resolverJefe(dto.getJefeBartenderId(), sede, TipoPersonal.BARTENDER, null))
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();

        return Mapper.toDTO(seccionRepository.save(seccion));
    }

    @Override
    @Transactional
    public SeccionDTO actualizar(Long id, SeccionDTO dto) {
        validarSeccionDTO(dto);
        Seccion seccion = seccionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));

        Sede sede = sedeRepository.findById(dto.getSedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede especificada no existe"));

        boolean cambiaSede = !seccion.getSede().getId().equals(sede.getId());

        if (cambiaSede && mesaRepository.existsBySeccionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cambiar de sede a una sección que tiene mesas"
            );
        }

        boolean estabaActiva = !Boolean.FALSE.equals(seccion.getActivo());
        boolean seraActiva = dto.getActivo() == null ? estabaActiva : dto.getActivo();

        if (estabaActiva && !seraActiva && mesaRepository.existsBySeccionIdAndActivaTrue(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Desactiva o mueve primero las mesas activas de esta sección"
            );
        }

        Personal jefeMozo = resolverJefe(dto.getJefeMozoId(), sede, TipoPersonal.MOZO, seccion.getJefeMozo());
        Personal jefeBartender = resolverJefe(dto.getJefeBartenderId(), sede, TipoPersonal.BARTENDER, seccion.getJefeBartender());

        seccion.setNombre(dto.getNombre().trim());
        seccion.setSede(sede);
        seccion.setJefeMozo(jefeMozo);
        seccion.setJefeBartender(jefeBartender);
        seccion.setActivo(seraActiva);

        if (dto.getOrden() != null) {
            seccion.setOrden(dto.getOrden());
        }

        return Mapper.toDTO(seccionRepository.save(seccion));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!seccionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada");
        }

        if (usuarioRepository.existsBySeccionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar la sección porque tiene usuarios asociados"
            );
        }
        if (mesaRepository.existsBySeccionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar la sección porque tiene mesas"
            );
        }

        seccionRepository.deleteById(id);
    }

    private Personal resolverJefe(Long jefeId, Sede sede, TipoPersonal tipo, Personal actual) {
        if (jefeId == null) {
            return null;
        }

        Personal personal = personalRepository.findById(jefeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El jefe seleccionado no existe"));

        if (!personal.getSede().getId().equals(sede.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jefe debe pertenecer a la misma sede de la sección");
        }

        if (personal.getTipo() != tipo) {
            String esperado = tipo == TipoPersonal.MOZO ? "mozo" : "bartender";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jefe seleccionado debe ser " + esperado);
        }

        boolean sigueIgual = actual != null && actual.getId().equals(personal.getId());

        if (!sigueIgual && !Boolean.TRUE.equals(personal.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jefe seleccionado está inactivo");
        }

        return personal;
    }

    private void validarSeccionDTO(SeccionDTO dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la sección es obligatorio");
        }
        if (dto.getSedeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de la sede es obligatorio");
        }
    }
}