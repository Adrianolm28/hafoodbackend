package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.domain.auditoria.EventoAuditoria;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.repository.EventoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LIMITE_TEXTO = 1000;

    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Usuario actor, Long sedeId, Long cuentaId, AccionAuditoria accion,
                          String antes, String despues, String motivo) {
        eventoAuditoriaRepository.save(EventoAuditoria.builder()
                .sedeId(sedeId)
                .cuentaId(cuentaId)
                .accion(accion)
                .usuarioId(actor.getId())
                .usuarioNombre(actor.getUsername())
                .antes(recortar(antes))
                .despues(recortar(despues))
                .motivo(motivo)
                .build());
    }

    private String recortar(String texto) {
        if (texto == null || texto.length() <= LIMITE_TEXTO) {
            return texto;
        }
        return texto.substring(0, LIMITE_TEXTO);
    }
}