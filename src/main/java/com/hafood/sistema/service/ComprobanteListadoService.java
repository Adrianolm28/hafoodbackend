package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ComprobanteResumenDTO;
import com.hafood.sistema.repository.ComprobanteRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComprobanteListadoService {

    private static final int LIMITE = 300;
    private static final long DIAS_MAXIMOS = 93;

    private final ComprobanteRepository comprobanteRepository;
    private final SedeAccesoService sedeAccesoService;

    @Transactional(readOnly = true)
    public List<ComprobanteResumenDTO> listar(Long sedeId, LocalDate desde, LocalDate hasta,
                                              EstadoSunat estado, TipoComprobante tipo, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);

        if (hasta.isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango máximo es de 3 meses");
        }

        Specification<Comprobante> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            filtros.add(cb.equal(root.get("sede").get("id"), sedeId));
            filtros.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), desde));
            filtros.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), hasta));

            if (estado != null) {
                filtros.add(cb.equal(root.get("estadoSunat"), estado));
            }
            if (tipo != null) {
                filtros.add(cb.equal(root.get("tipo"), tipo));
            }

            return cb.and(filtros.toArray(new Predicate[0]));
        };

        return comprobanteRepository
                .findAll(spec, PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "emitidoEn", "id")))
                .getContent().stream()
                .map(c -> new ComprobanteResumenDTO(
                        c.getId(),
                        c.getCuenta().getId(),
                        c.getSede().getId(),
                        c.getTipo(),
                        c.getSerie() + "-" + String.format("%08d", c.getCorrelativo()),
                        c.getFechaEmision(),
                        c.getEmitidoEn(),
                        c.getEstadoSunat(),
                        c.getCliente().getNombre(),
                        c.getCliente().getTipoDocumento(),
                        c.getCliente().getNumeroDocumento(),
                        c.getTotal()))
                .toList();
    }
}