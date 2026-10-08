package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.domain.caja.Caja;
import com.hafood.sistema.domain.caja.CajaSeccion;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CajaDTO;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.mapper.CajaMapper;
import com.hafood.sistema.repository.CajaRepository;
import com.hafood.sistema.repository.CajaSeccionRepository;
import com.hafood.sistema.repository.CajaSesionRepository;
import com.hafood.sistema.repository.SeccionRepository;
import com.hafood.sistema.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaRepository cajaRepository;
    private final CajaSeccionRepository cajaSeccionRepository;
    private final CajaSesionRepository cajaSesionRepository;
    private final SeccionRepository seccionRepository;
    private final SedeRepository sedeRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AutorizacionService autorizacionService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<CajaDTO> listar(Long sedeId, boolean incluirInactivas, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        boolean supervisor = autorizacionService.esSupervisor(actor);

        List<Caja> cajas = incluirInactivas
                ? cajaRepository.findBySedeIdOrderByNombreAsc(sedeId)
                : cajaRepository.findBySedeIdAndActivaTrueOrderByNombreAsc(sedeId);

        if (cajas.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CajaDTO.SeccionRef>> secciones = cajaSeccionRepository
                .findByCajaIds(cajas.stream().map(Caja::getId).toList()).stream()
                .collect(Collectors.groupingBy(cs -> cs.getCaja().getId(),
                        Collectors.mapping(cs -> new CajaDTO.SeccionRef(cs.getSeccion().getId(), cs.getSeccion().getNombre()),
                                Collectors.toList())));

        List<Long> sesionIds = cajas.stream().map(Caja::getSesionAbiertaId).filter(Objects::nonNull).toList();
        Map<Long, CajaSesion> sesiones = cajaSesionRepository.findAllById(sesionIds).stream()
                .collect(Collectors.toMap(CajaSesion::getId, Function.identity()));

        return cajas.stream().map(c -> {
            CajaSesion sesion = c.getSesionAbiertaId() == null ? null : sesiones.get(c.getSesionAbiertaId());
            return aDTO(c, secciones.getOrDefault(c.getId(), List.of()),
                    sesion == null ? null : CajaMapper.toResumen(sesion, supervisor));
        }).toList();
    }

    @Transactional
    public CajaDTO crear(CajaRequests.Crear request, Usuario actor) {
        Sede sede = sedeRepository.findById(request.sedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede no existe"));
        sedeAccesoService.exigirAcceso(actor, sede.getId());

        String nombre = request.nombre().trim();

        if (cajaRepository.existsBySedeIdAndNombreIgnoreCase(sede.getId(), nombre)) {
            throw conflicto("Ya existe una caja con ese nombre en esta sede");
        }

        List<Seccion> secciones = validarSecciones(request.seccionIds(), sede.getId(), null);
        Caja caja = cajaRepository.save(Caja.builder().sede(sede).nombre(nombre).build());
        vincular(caja, secciones);

        auditoriaService.registrar(actor, sede.getId(), null, AccionAuditoria.CAJA_CREADA, null,
                "caja=" + nombre + ";secciones=" + nombres(secciones), null);

        return aDTO(caja, refs(secciones), null);
    }

    @Transactional
    public CajaDTO actualizar(Long id, CajaRequests.Actualizar request, Usuario actor) {
        Caja caja = cajaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La caja no existe"));
        Long sedeId = caja.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);

        String nombre = request.nombre().trim();

        if (cajaRepository.existsBySedeIdAndNombreIgnoreCaseAndIdNot(sedeId, nombre, id)) {
            throw conflicto("Ya existe una caja con ese nombre en esta sede");
        }

        List<Seccion> secciones = validarSecciones(request.seccionIds(), sedeId, id);
        Set<Long> nuevas = secciones.stream().map(Seccion::getId).collect(Collectors.toSet());
        Set<Long> actuales = cajaSeccionRepository.findByCajaIds(List.of(id)).stream()
                .map(cs -> cs.getSeccion().getId()).collect(Collectors.toSet());

        if (caja.getSesionAbiertaId() != null && (!request.activa() || !nuevas.equals(new HashSet<>(actuales)))) {
            throw conflicto("La caja está abierta. Ciérrala antes de desactivarla o cambiar sus secciones");
        }

        String antes = "caja=" + caja.getNombre() + ";activa=" + caja.isActiva();
        caja.setNombre(nombre);
        caja.setActiva(request.activa());
        cajaRepository.save(caja);

        if (!nuevas.equals(new HashSet<>(actuales))) {
            cajaSeccionRepository.eliminarPorCajaId(id);
            vincular(caja, secciones);
        }

        auditoriaService.registrar(actor, sedeId, null, AccionAuditoria.CAJA_ACTUALIZADA, antes,
                "caja=" + nombre + ";activa=" + request.activa() + ";secciones=" + nombres(secciones), null);

        CajaSesion sesion = caja.getSesionAbiertaId() == null
                ? null : cajaSesionRepository.findById(caja.getSesionAbiertaId()).orElse(null);

        return aDTO(caja, refs(secciones), sesion == null ? null : CajaMapper.toResumen(sesion, true));
    }

    private List<Seccion> validarSecciones(List<Long> ids, Long sedeId, Long cajaId) {
        return ids.stream().distinct().map(id -> {
            Seccion seccion = seccionRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección no existe"));

            if (!seccion.getSede().getId().equals(sedeId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La sección «" + seccion.getNombre() + "» pertenece a otra sede");
            }
            if (Boolean.FALSE.equals(seccion.getActivo())) {
                throw conflicto("La sección «" + seccion.getNombre() + "» está inactiva");
            }

            cajaSeccionRepository.findBySeccionIdConCaja(id).ifPresent(otra -> {
                if (!Objects.equals(otra.getCaja().getId(), cajaId)) {
                    throw conflicto("La sección «" + seccion.getNombre() + "» ya está asignada a la caja «"
                            + otra.getCaja().getNombre() + "»");
                }
            });

            return seccion;
        }).toList();
    }

    private void vincular(Caja caja, List<Seccion> secciones) {
        try {
            cajaSeccionRepository.saveAllAndFlush(secciones.stream()
                    .map(s -> CajaSeccion.builder().caja(caja).seccion(s).build())
                    .toList());
        } catch (DataIntegrityViolationException e) {
            throw conflicto("Una de las secciones ya pertenece a otra caja");
        }
    }

    private List<CajaDTO.SeccionRef> refs(List<Seccion> secciones) {
        return secciones.stream().map(s -> new CajaDTO.SeccionRef(s.getId(), s.getNombre())).toList();
    }

    private String nombres(List<Seccion> secciones) {
        return secciones.stream().map(Seccion::getNombre).collect(Collectors.joining(", "));
    }

    private CajaDTO aDTO(Caja caja, List<CajaDTO.SeccionRef> secciones, CajaDTO.SesionResumen sesion) {
        return new CajaDTO(caja.getId(), caja.getSede().getId(), caja.getNombre(), caja.isActiva(), secciones, sesion);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}