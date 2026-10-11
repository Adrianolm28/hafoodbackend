package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteArchivo;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.EnvioMasivoDTO;
import com.hafood.sistema.dto.EnvioResultadoDTO;
import com.hafood.sistema.repository.ComprobanteArchivoRepository;
import com.hafood.sistema.repository.ComprobanteRepository;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SunatEnvioService {

    private static final int LIMITE_TEXTO = 500;
    private static final int LIMITE_MASIVO = 40;
    private static final int FALLOS_SEGUIDOS_MAXIMOS = 3;
    private static final List<EstadoSunat> REENVIABLES = List.of(EstadoSunat.PENDIENTE, EstadoSunat.ERROR_ENVIO);

    private record Preparado(String ruc, String nombreBase, byte[] xml, AmbienteSunat ambiente,
                             SunatGateway.Credenciales credenciales) {
    }

    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteArchivoRepository archivoRepository;
    private final ConfiguracionSunatRepository configRepository;
    private final CifradoService cifradoService;
    private final SunatGateway gateway;
    private final UblComprobanteGenerador generador;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;
    private final PlatformTransactionManager transactionManager;
    private final Set<String> enCurso = ConcurrentHashMap.newKeySet();

    public EnvioResultadoDTO enviar(Long comprobanteId, Usuario actor) {
        TransactionTemplate lectura = new TransactionTemplate(transactionManager);
        lectura.setReadOnly(true);
        Preparado preparado = lectura.execute(estado -> preparar(comprobanteId, actor));
        String clave = preparado.ruc() + ":" + comprobanteId;

        if (!enCurso.add(clave)) {
            throw conflicto("Este comprobante ya se está enviando. Espera unos segundos");
        }

        try {
            SunatGateway.RespuestaSunat respuesta = gateway.enviarComprobante(
                    preparado.ambiente(), preparado.credenciales(), preparado.nombreBase(), preparado.xml());

            return new TransactionTemplate(transactionManager)
                    .execute(estado -> registrar(comprobanteId, actor, respuesta));
        } finally {
            enCurso.remove(clave);
        }
    }

    public EnvioMasivoDTO enviarPendientes(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        List<Long> ids = comprobanteRepository.findIdsParaEnviar(sedeId, REENVIABLES,
                PageRequest.of(0, LIMITE_MASIVO + 1));
        boolean quedanMas = ids.size() > LIMITE_MASIVO;
        int aceptados = 0;
        int rechazados = 0;
        int conError = 0;
        int omitidos = 0;
        int seguidos = 0;
        String motivo = null;

        for (Long id : ids.stream().limit(LIMITE_MASIVO).toList()) {
            try {
                EnvioResultadoDTO r = enviar(id, actor);

                switch (r.estado()) {
                    case ACEPTADO, ACEPTADO_CON_OBSERVACIONES -> aceptados++;
                    case RECHAZADO -> rechazados++;
                    default -> conError++;
                }

                seguidos = r.estado() == EstadoSunat.ERROR_ENVIO ? seguidos + 1 : 0;
            } catch (ResponseStatusException e) {
                omitidos++;
                seguidos = 0;

                if (motivo == null) {
                    motivo = e.getReason();
                }
            }

            if (seguidos >= FALLOS_SEGUIDOS_MAXIMOS) {
                quedanMas = true;
                break;
            }
        }

        return new EnvioMasivoDTO(aceptados, rechazados, conError, omitidos, quedanMas, motivo);
    }

    private Preparado preparar(Long comprobanteId, Usuario actor) {
        Comprobante c = comprobanteRepository.findById(comprobanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El comprobante no existe"));
        sedeAccesoService.exigirAcceso(actor, c.getSede().getId());

        if (!REENVIABLES.contains(c.getEstadoSunat())) {
            throw conflicto("Este comprobante ya tiene respuesta de SUNAT y no se vuelve a enviar");
        }

        ConfiguracionSunat config = configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA)
                .orElseThrow(() -> conflicto("La facturación electrónica no está configurada"));

        if (!config.isActiva()) {
            throw conflicto("La facturación electrónica no está activa");
        }
        if (!config.getRuc().equals(c.getRucEmisor())) {
            throw conflicto("El RUC configurado no coincide con el del comprobante");
        }
        if (config.getAmbiente() != c.getAmbiente()) {
            throw conflicto("El comprobante se emitió en ambiente " + c.getAmbiente()
                    + " y hoy la empresa trabaja en " + config.getAmbiente());
        }
        if (config.getAmbiente() == AmbienteSunat.PRODUCCION
                && (config.getUsuarioSol() == null || config.getClaveSolCifrada() == null)) {
            throw conflicto("Faltan el usuario y la clave SOL en Facturación");
        }

        ComprobanteArchivo archivo = archivoRepository.findByComprobanteId(comprobanteId)
                .orElseThrow(() -> conflicto("El comprobante aún no tiene XML. Prepáralo de nuevo"));

        if (!archivo.isFirmado() || archivo.getXml() == null) {
            throw conflicto(archivo.getErrorGeneracion() == null
                    ? "El comprobante aún no está firmado"
                    : "El comprobante no está firmado: " + archivo.getErrorGeneracion());
        }

        return new Preparado(config.getRuc(), generador.nombreBase(c), archivo.getXml(), c.getAmbiente(),
                credenciales(config));
    }

    private SunatGateway.Credenciales credenciales(ConfiguracionSunat config) {
        if (config.getAmbiente() == AmbienteSunat.BETA) {
            return new SunatGateway.Credenciales(config.getRuc() + "MODDATOS", "MODDATOS");
        }

        return new SunatGateway.Credenciales(config.getRuc() + config.getUsuarioSol(),
                cifradoService.descifrarTexto(config.getClaveSolCifrada()));
    }

    private EnvioResultadoDTO registrar(Long comprobanteId, Usuario actor, SunatGateway.RespuestaSunat r) {
        Comprobante c = comprobanteRepository.findById(comprobanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El comprobante no existe"));
        ComprobanteArchivo a = archivoRepository.findByComprobanteId(comprobanteId)
                .orElseThrow(() -> conflicto("El comprobante no tiene archivo"));

        if (!REENVIABLES.contains(c.getEstadoSunat())) {
            return new EnvioResultadoDTO(c.getEstadoSunat(), a.getCodigoRespuesta(), a.getMensajeRespuesta());
        }

        EstadoSunat antes = c.getEstadoSunat();
        String mensaje = recortar(r.mensaje());

        a.setIntentos((a.getIntentos() == null ? 0 : a.getIntentos()) + 1);
        a.setEnviadoEn(Instant.now());
        a.setCodigoRespuesta(r.codigo());
        a.setMensajeRespuesta(mensaje);

        if (r.cdrZip() != null) {
            a.setCdr(r.cdrZip());
            a.setNombreCdr("R-" + generador.nombreBase(c) + ".zip");
        }

        c.setEstadoSunat(r.estado());
        archivoRepository.save(a);
        comprobanteRepository.save(c);

        auditoriaService.registrar(actor, c.getSede().getId(), c.getCuenta().getId(),
                AccionAuditoria.COMPROBANTE_ENVIADO,
                "estado=" + antes,
                "numero=" + generador.numero(c) + ";estado=" + r.estado() + ";codigo=" + r.codigo()
                        + ";intento=" + a.getIntentos(),
                null);

        return new EnvioResultadoDTO(r.estado(), r.codigo(), mensaje);
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }

        return texto.length() <= LIMITE_TEXTO ? texto : texto.substring(0, LIMITE_TEXTO);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}