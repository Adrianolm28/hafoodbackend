package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.sunat.SedeSunat;
import com.hafood.sistema.domain.sunat.SerieComprobante;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.SedeSunatDTO;
import com.hafood.sistema.dto.request.SunatRequests;
import com.hafood.sistema.repository.SedeSunatRepository;
import com.hafood.sistema.repository.SerieComprobanteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SunatSedeService {

    @PersistenceContext
    private EntityManager entityManager;

    private final SedeSunatRepository sedeSunatRepository;
    private final SerieComprobanteRepository serieRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public SedeSunatDTO obtener(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        Sede sede = exigirSede(sedeId);
        return armar(sede, sedeSunatRepository.findBySedeId(sedeId).orElse(null), serieRepository.findBySedeId(sedeId));
    }

    @Transactional
    public SedeSunatDTO guardar(Long sedeId, SunatRequests.Establecimiento request, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        Sede sede = exigirSede(sedeId);
        SedeSunat existente = sedeSunatRepository.findBySedeId(sedeId).orElse(null);
        List<SerieComprobante> seriesAntes = serieRepository.findBySedeId(sedeId);
        String antes = existente == null ? null : resumen(existente, seriesAntes);

        SedeSunat fiscal = existente != null ? existente : new SedeSunat();
        fiscal.setSede(sede);
        fiscal.setCodigoEstablecimiento(request.codigoEstablecimiento());
        fiscal.setDireccionFiscal(request.direccionFiscal().trim());
        fiscal.setUbigeo(request.ubigeo());

        try {
            sedeSunatRepository.saveAndFlush(fiscal);
            aplicarSerie(sede, seriesAntes, TipoComprobante.FACTURA, request.serieFactura());
            aplicarSerie(sede, seriesAntes, TipoComprobante.BOLETA, request.serieBoleta());
        } catch (DataIntegrityViolationException e) {
            throw conflicto("Esa serie ya la usa otra sede o la configuración cambió. Actualiza la pantalla");
        }

        List<SerieComprobante> seriesDespues = serieRepository.findBySedeId(sedeId);

        auditoriaService.registrar(actor, sedeId, null, AccionAuditoria.SUNAT_SEDE_ACTUALIZADA,
                antes, resumen(fiscal, seriesDespues), null);

        return armar(sede, fiscal, seriesDespues);
    }

    private void aplicarSerie(Sede sede, List<SerieComprobante> actuales, TipoComprobante tipo, String serie) {
        SerieComprobante actual = actuales.stream().filter(s -> s.getTipo() == tipo).findFirst().orElse(null);

        if (actual != null && actual.getSerie().equals(serie)) {
            return;
        }
        if (actual != null && actual.getUltimoCorrelativo() > 0) {
            throw conflicto("La serie " + actual.getSerie() + " ya emitió comprobantes y no se puede cambiar");
        }
        if (serieRepository.existsBySerie(serie)) {
            throw conflicto("La serie " + serie + " ya la usa otra sede");
        }

        SerieComprobante nueva = actual != null ? actual : new SerieComprobante();
        nueva.setSede(sede);
        nueva.setTipo(tipo);
        nueva.setSerie(serie);
        serieRepository.saveAndFlush(nueva);
    }

    private Sede exigirSede(Long sedeId) {
        Sede sede = entityManager.find(Sede.class, sedeId);

        if (sede == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede no existe");
        }

        return sede;
    }

    private String serieDe(List<SerieComprobante> series, TipoComprobante tipo) {
        return series.stream().filter(s -> s.getTipo() == tipo).map(SerieComprobante::getSerie).findFirst().orElse(null);
    }

    private String resumen(SedeSunat fiscal, List<SerieComprobante> series) {
        return "establecimiento=" + fiscal.getCodigoEstablecimiento()
                + ";ubigeo=" + fiscal.getUbigeo()
                + ";direccion=" + fiscal.getDireccionFiscal()
                + ";serieFactura=" + serieDe(series, TipoComprobante.FACTURA)
                + ";serieBoleta=" + serieDe(series, TipoComprobante.BOLETA);
    }

    private SedeSunatDTO armar(Sede sede, SedeSunat fiscal, List<SerieComprobante> series) {
        return new SedeSunatDTO(
                sede.getId(),
                sede.getNombre(),
                fiscal != null,
                fiscal == null ? null : fiscal.getCodigoEstablecimiento(),
                fiscal == null ? null : fiscal.getDireccionFiscal(),
                fiscal == null ? null : fiscal.getUbigeo(),
                serieDe(series, TipoComprobante.FACTURA),
                serieDe(series, TipoComprobante.BOLETA));
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}