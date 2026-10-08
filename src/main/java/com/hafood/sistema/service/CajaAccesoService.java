package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoCajaSesion;
import com.hafood.sistema.domain.caja.CajaSesion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.repository.CajaSesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CajaAccesoService {

    private final CajaSesionRepository cajaSesionRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AutorizacionService autorizacionService;

    @Transactional(propagation = Propagation.MANDATORY)
    public CajaSesion bloquear(Long sesionId, Usuario actor) {
        CajaSesion sesion = cajaSesionRepository.findByIdForUpdate(sesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La jornada de caja no existe"));
        sedeAccesoService.exigirAcceso(actor, sesion.getSede().getId());
        exigirOperador(sesion, actor);
        return sesion;
    }

    public void exigirOperador(CajaSesion sesion, Usuario actor) {
        if (!autorizacionService.esSupervisor(actor) && !sesion.getAbiertaPor().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta caja la tiene otra cajera. Pide el pase de turno");
        }
    }

    public void exigirAbierta(CajaSesion sesion) {
        if (sesion.getEstado() != EstadoCajaSesion.ABIERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La jornada de caja ya está cerrada");
        }
    }
}