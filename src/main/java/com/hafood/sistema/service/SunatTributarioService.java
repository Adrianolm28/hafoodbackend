package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoImpuesto;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.sunat.TasaImpuesto;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.TasaImpuestoDTO;
import com.hafood.sistema.dto.TributarioDTO;
import com.hafood.sistema.dto.request.SunatRequests;
import com.hafood.sistema.repository.ComprobanteRepository;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import com.hafood.sistema.repository.TasaImpuestoRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SunatTributarioService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ConfiguracionSunatRepository configRepository;
    private final TasaImpuestoRepository tasaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public TributarioDTO obtener() {
        ConfiguracionSunat config = configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA).orElse(null);
        return armar(config);
    }

    @Transactional
    public TributarioDTO guardar(SunatRequests.Tributario request, Usuario actor) {
        ConfiguracionSunat config = exigirConfigBloqueada();
        RegimenTributario anterior = config.getRegimen();
        LocalDate hoy = LocalDate.now(LIMA);

        if (config.isActiva() && request.regimen() != anterior
                && tasaRepository.findVigentes(request.regimen(), TipoImpuesto.IGV, hoy).size() != 1) {
            throw conflicto("Primero registra la tasa de IGV vigente del régimen " + request.regimen());
        }

        String antes = resumen(config);

        config.setRegimen(request.regimen());
        config.setUmbralBoletaSinDocumento(request.umbralBoletaSinDocumento() == null
                ? null
                : request.umbralBoletaSinDocumento().setScale(2, RoundingMode.HALF_UP));
        config.setActualizadoPor(usuarioRepository.getReferenceById(actor.getId()));
        config.setActualizadoEn(Instant.now());
        configRepository.save(config);

        auditoriaService.registrar(actor, null, null, AccionAuditoria.SUNAT_FISCAL_ACTUALIZADA,
                antes, resumen(config), null);

        return armar(config);
    }

    @Transactional
    public TributarioDTO crearTasa(SunatRequests.Tasa request, Usuario actor) {
        ConfiguracionSunat config = exigirConfigBloqueada();
        LocalDate desde = request.vigenteDesde();
        LocalDate hasta = request.vigenteHasta();
        LocalDate hoy = LocalDate.now(LIMA);

        if (hasta != null && hasta.isBefore(desde)) {
            throw invalida("La fecha final no puede ser anterior a la fecha de inicio");
        }
        if (desde.isBefore(hoy)
                && comprobanteRepository.existsByRegimenAndFechaEmisionGreaterThanEqual(request.regimen(), desde)) {
            throw conflicto("Ya hay comprobantes emitidos desde esa fecha. La tasa debe empezar hoy o después");
        }

        List<TasaImpuesto> existentes = tasaRepository
                .findByRegimenAndTipoOrderByVigenteDesde(request.regimen(), request.tipo());
        List<TasaImpuesto> cruzadas = existentes.stream().filter(t -> seCruzan(t, desde, hasta)).toList();
        String antes = null;

        if (!cruzadas.isEmpty()) {
            TasaImpuesto previa = cruzadas.get(0);
            boolean reemplazable = cruzadas.size() == 1
                    && hasta == null
                    && previa.getVigenteHasta() == null
                    && previa.getVigenteDesde().isBefore(desde);

            if (!reemplazable) {
                throw conflicto("Las fechas se cruzan con otra tasa de " + request.tipo()
                        + ". Ajusta las fechas o crea la nueva sin fecha final para reemplazar a la vigente");
            }

            antes = "tasa=#" + previa.getId() + ";porcentaje=" + previa.getPorcentaje().toPlainString()
                    + ";desde=" + previa.getVigenteDesde() + ";hasta=sin fin";
            previa.setVigenteHasta(desde.minusDays(1));
            tasaRepository.save(previa);
        }

        TasaImpuesto nueva = tasaRepository.save(TasaImpuesto.builder()
                .regimen(request.regimen())
                .tipo(request.tipo())
                .porcentaje(request.porcentaje().setScale(2, RoundingMode.HALF_UP))
                .vigenteDesde(desde)
                .vigenteHasta(hasta)
                .creadoPorId(actor.getId())
                .build());

        auditoriaService.registrar(actor, null, null, AccionAuditoria.TASA_IMPUESTO_CREADA, antes,
                "regimen=" + nueva.getRegimen() + ";tipo=" + nueva.getTipo()
                        + ";porcentaje=" + nueva.getPorcentaje().toPlainString()
                        + ";desde=" + nueva.getVigenteDesde()
                        + ";hasta=" + (nueva.getVigenteHasta() == null ? "sin fin" : nueva.getVigenteHasta()),
                null);

        return armar(config);
    }

    private boolean seCruzan(TasaImpuesto tasa, LocalDate desde, LocalDate hasta) {
        LocalDate finNueva = hasta == null ? LocalDate.MAX : hasta;
        return !tasa.getVigenteDesde().isAfter(finNueva)
                && (tasa.getVigenteHasta() == null || !tasa.getVigenteHasta().isBefore(desde));
    }

    private ConfiguracionSunat exigirConfigBloqueada() {
        return configRepository.findByClaveUnicaForUpdate(ConfiguracionSunat.CLAVE_UNICA)
                .orElseThrow(() -> conflicto("Primero guarda los datos de la empresa"));
    }

    private TributarioDTO armar(ConfiguracionSunat config) {
        LocalDate hoy = LocalDate.now(LIMA);
        List<TasaImpuestoDTO> tasas = tasaRepository.findAllByOrderByRegimenAscTipoAscVigenteDesdeAsc().stream()
                .map(t -> new TasaImpuestoDTO(t.getId(), t.getRegimen(), t.getTipo(), t.getPorcentaje(),
                        t.getVigenteDesde(), t.getVigenteHasta(),
                        !t.getVigenteDesde().isAfter(hoy)
                                && (t.getVigenteHasta() == null || !t.getVigenteHasta().isBefore(hoy))))
                .toList();

        return new TributarioDTO(
                config != null,
                config == null ? null : config.getRegimen(),
                config == null ? null : config.getUmbralBoletaSinDocumento(),
                tasas);
    }

    private String resumen(ConfiguracionSunat config) {
        BigDecimal umbral = config.getUmbralBoletaSinDocumento();
        return "regimen=" + config.getRegimen() + ";umbralBoleta=" + (umbral == null ? "sin límite" : umbral.toPlainString());
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}