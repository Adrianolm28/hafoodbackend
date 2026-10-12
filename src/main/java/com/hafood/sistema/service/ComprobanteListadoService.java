package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ComprobanteResumenDTO;
import com.hafood.sistema.repository.ComprobanteArchivoRepository;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComprobanteListadoService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final int LIMITE = 300;
    private static final long DIAS_MAXIMOS = 93;

    private record EstadoArchivo(boolean firmado, boolean tieneCdr, String codigo, String mensaje,
                                 String error, int intentos) {
    }

    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteArchivoRepository archivoRepository;
    private final SedeAccesoService sedeAccesoService;
    private final PlazosEnvio plazos;

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

        List<Comprobante> comprobantes = comprobanteRepository
                .findAll(spec, PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "emitidoEn", "id")))
                .getContent();
        Map<Long, EstadoArchivo> archivos = estados(comprobantes);
        LocalDate hoy = LocalDate.now(ZONA);

        return comprobantes.stream().map(c -> resumen(c, archivos.get(c.getId()), hoy)).toList();
    }

    private Map<Long, EstadoArchivo> estados(List<Comprobante> comprobantes) {
        if (comprobantes.isEmpty()) {
            return Map.of();
        }

        Map<Long, EstadoArchivo> mapa = new HashMap<>();

        for (Object[] f : archivoRepository.findEstadosPorComprobanteIds(
                comprobantes.stream().map(Comprobante::getId).toList())) {
            mapa.put((Long) f[0], new EstadoArchivo(Boolean.TRUE.equals(f[1]), Boolean.TRUE.equals(f[6]),
                    (String) f[2], (String) f[3], (String) f[4], f[5] == null ? 0 : ((Number) f[5]).intValue()));
        }

        return mapa;
    }

    private ComprobanteResumenDTO resumen(Comprobante c, EstadoArchivo a, LocalDate hoy) {
        LocalDate vence = c.getFechaEmision().plusDays(plazos.dias(c.getTipo()));
        String mensaje = null;

        if (a != null && a.mensaje() != null) {
            mensaje = a.codigo() == null ? a.mensaje() : a.codigo() + ": " + a.mensaje();
        }

        return new ComprobanteResumenDTO(
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
                c.getTotal(),
                a != null && a.firmado(),
                a != null && a.tieneCdr(),
                mensaje,
                a == null ? null : a.error(),
                a == null ? 0 : a.intentos(),
                vence,
                ChronoUnit.DAYS.between(hoy, vence));
    }
}