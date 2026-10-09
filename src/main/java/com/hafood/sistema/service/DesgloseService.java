package com.hafood.sistema.service;

import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.sunat.TasaImpuesto;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.DesgloseDTO;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.TasaImpuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesgloseService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final CuentaRepository cuentaRepository;
    private final CuentaCalculoService calculoService;
    private final ConfiguracionSunatRepository configuracionRepository;
    private final TasaImpuestoRepository tasaRepository;
    private final SedeAccesoService sedeAccesoService;

    @Transactional(readOnly = true)
    public DesgloseDTO obtener(Long cuentaId, Usuario actor) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe"));
        sedeAccesoService.exigirAcceso(actor, cuenta.getSede().getId());

        ConfiguracionSunat config = configuracionRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA).orElse(null);

        if (config == null || config.getRegimen() == null) {
            return noDisponible("Falta configurar el régimen tributario");
        }

        RegimenTributario regimen = config.getRegimen();
        LocalDate hoy = LocalDate.now(ZONA);
        List<TasaImpuesto> igvVigentes = tasaRepository.findVigentes(regimen, TipoImpuesto.IGV, hoy);
        List<TasaImpuesto> ipmVigentes = tasaRepository.findVigentes(regimen, TipoImpuesto.IPM, hoy);

        if (igvVigentes.size() != 1 || ipmVigentes.size() > 1) {
            return noDisponible("Falta una tasa de impuesto vigente");
        }

        BigDecimal igvPct = igvVigentes.get(0).getPorcentaje();
        BigDecimal ipmPct = ipmVigentes.isEmpty() ? BigDecimal.ZERO.setScale(2) : ipmVigentes.get(0).getPorcentaje();

        List<CuentaLinea> vivas = calculoService.recalcular(cuenta).lineas().stream()
                .filter(l -> l.getTotalLinea() != null && l.getTotalLinea().signum() > 0)
                .toList();

        if (vivas.isEmpty()) {
            return noDisponible("La cuenta no tiene productos con importe");
        }

        List<DesgloseDTO.Linea> lineas = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal ipm = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (CuentaLinea l : vivas) {
            ImpuestoCalculador.Desglose d = ImpuestoCalculador.desglosar(l.getTotalLinea(), igvPct, ipmPct);
            BigDecimal descuento = nvl(l.getDescuentoLinea()).add(nvl(l.getDescuentoCuenta()));
            lineas.add(new DesgloseDTO.Linea(l.getId(), l.getNombre(), l.getCantidad(), l.getPrecioUnitario(),
                    descuento, l.getTotalLinea(), d.base(), d.igv(), d.ipm()));
            base = base.add(d.base());
            igv = igv.add(d.igv());
            ipm = ipm.add(d.ipm());
            total = total.add(l.getTotalLinea());
        }

        return new DesgloseDTO(true, null, regimen, igvPct, ipmPct, cuenta.getSubtotal(), cuenta.getDescuentoTotal(),
                base, igv, ipm, total, lineas);
    }

    private DesgloseDTO noDisponible(String motivo) {
        return new DesgloseDTO(false, motivo, null, null, null, null, null, null, null, null, null, List.of());
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}