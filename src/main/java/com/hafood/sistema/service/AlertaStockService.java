package com.hafood.sistema.service;

import com.hafood.sistema.domain.pos.AlertaStock;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.AlertaStockDTO;
import com.hafood.sistema.repository.AlertaStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaStockService {

    private static final int LIMITE = 200;

    private final AlertaStockRepository alertaStockRepository;
    private final SedeAccesoService sedeAccesoService;

    @Transactional(readOnly = true)
    public List<AlertaStockDTO> listar(Long sedeId, boolean soloPendientes, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        Pageable pagina = PageRequest.of(0, LIMITE);

        List<AlertaStock> alertas = soloPendientes
                ? alertaStockRepository.findBySedeIdAndRevisadaFalseOrderByCreadaEnDesc(sedeId, pagina)
                : alertaStockRepository.findBySedeIdOrderByCreadaEnDesc(sedeId, pagina);

        return alertas.stream().map(this::aDTO).toList();
    }

    @Transactional
    public AlertaStockDTO revisar(Long id, Usuario actor) {
        AlertaStock alerta = alertaStockRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La alerta no existe"));
        sedeAccesoService.exigirAcceso(actor, alerta.getSedeId());

        if (!alerta.isRevisada()) {
            alerta.setRevisada(true);
            alerta.setRevisadaPorNombre(actor.getUsername());
            alerta.setRevisadaEn(Instant.now());
            alertaStockRepository.save(alerta);
        }

        return aDTO(alerta);
    }

    private AlertaStockDTO aDTO(AlertaStock a) {
        return new AlertaStockDTO(a.getId(), a.getCuentaId(), a.getLineaId(), a.getInsumoId(), a.getTipo(),
                a.getCantidadFaltante(), a.getDescripcion(), a.getCreadaEn(), a.isRevisada(),
                a.getRevisadaPorNombre(), a.getRevisadaEn());
    }
}