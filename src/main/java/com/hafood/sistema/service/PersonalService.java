package com.hafood.sistema.service;

import com.hafood.sistema.domain.estructura.Personal;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.PersonalDTO;
import com.hafood.sistema.dto.SiguienteCodigoDTO;
import com.hafood.sistema.mapper.PersonalMapper;
import com.hafood.sistema.repository.PersonalRepository;
import com.hafood.sistema.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalService {

    private static final int CODIGO_MAXIMO = 9999;

    private final PersonalRepository personalRepository;
    private final SedeRepository sedeRepository;
    private final SedeAccesoService sedeAccesoService;

    @Transactional(readOnly = true)
    public Page<PersonalDTO> listar(Long sedeId, boolean soloActivos, Pageable pageable, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        Page<Personal> page = soloActivos
                ? personalRepository.findBySedeIdAndActivoTrue(sedeId, pageable)
                : personalRepository.findBySedeId(sedeId, pageable);
        return page.map(PersonalMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<PersonalDTO> listarActivos(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        return personalRepository.findBySedeIdAndActivoTrueOrderByCodigoAsc(sedeId)
                .stream()
                .map(PersonalMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiguienteCodigoDTO siguienteCodigo(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        return new SiguienteCodigoDTO(calcularSiguienteCodigo(sedeId));
    }

    @Transactional
    public PersonalDTO crear(PersonalDTO dto, Usuario actor) {
        Sede sede = sedeRepository.findById(dto.getSedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede especificada no existe"));
        sedeAccesoService.exigirAcceso(actor, sede.getId());

        int codigo = dto.getCodigo() != null ? dto.getCodigo() : calcularSiguienteCodigo(sede.getId());

        if (personalRepository.existsBySedeIdAndCodigo(sede.getId(), codigo)) {
            throw codigoEnUso();
        }

        Personal personal = Personal.builder()
                .sede(sede)
                .codigo(codigo)
                .nombre(dto.getNombre().trim())
                .tipo(dto.getTipo())
                .build();

        return guardar(personal);
    }

    @Transactional
    public PersonalDTO actualizar(Long id, PersonalDTO dto, Usuario actor) {
        Personal personal = buscar(id);
        Long sedeId = personal.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);

        if (!sedeId.equals(dto.getSedeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar de sede al personal");
        }

        boolean seraActivo = dto.getActivo() == null ? Boolean.TRUE.equals(personal.getActivo()) : dto.getActivo();
        boolean dejaDeEstarActivo = Boolean.TRUE.equals(personal.getActivo()) && !seraActivo;
        boolean cambiaCargo = dto.getTipo() != personal.getTipo();

        if ((dejaDeEstarActivo || cambiaCargo) && personalRepository.esJefeDeSeccion(id)) {
            throw jefeDeSeccion();
        }

        if (dto.getCodigo() != null
                && !dto.getCodigo().equals(personal.getCodigo())
                && personalRepository.existsBySedeIdAndCodigoAndIdNot(sedeId, dto.getCodigo(), id)) {
            throw codigoEnUso();
        }

        if (dto.getCodigo() != null) {
            personal.setCodigo(dto.getCodigo());
        }
        personal.setNombre(dto.getNombre().trim());
        personal.setTipo(dto.getTipo());

        if (dto.getActivo() != null) {
            personal.setActivo(dto.getActivo());
        }

        return guardar(personal);
    }

    @Transactional
    public void desactivar(Long id, Usuario actor) {
        Personal personal = buscar(id);
        sedeAccesoService.exigirAcceso(actor, personal.getSede().getId());
        if (personalRepository.esJefeDeSeccion(id)) {
            throw jefeDeSeccion();
        }
        personal.setActivo(false);
        personalRepository.save(personal);
    }

    private Personal buscar(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El personal no existe"));
    }

    private int calcularSiguienteCodigo(Long sedeId) {
        Integer maximo = personalRepository.findMaxCodigoBySedeId(sedeId);
        int siguiente = maximo == null ? 1 : maximo + 1;

        if (siguiente > CODIGO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No quedan códigos disponibles en esta sede");
        }

        return siguiente;
    }

    private PersonalDTO guardar(Personal personal) {
        try {
            return PersonalMapper.toDTO(personalRepository.saveAndFlush(personal));
        } catch (DataIntegrityViolationException e) {
            throw codigoEnUso();
        }
    }

    private ResponseStatusException jefeDeSeccion() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Es jefe de una sección. Quítalo de la sección antes de desactivarlo o cambiarle el cargo"
        );
    }

    private ResponseStatusException codigoEnUso() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ese código ya está en uso en esta sede");
    }
}