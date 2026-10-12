package com.hafood.sistema.service;

import com.hafood.sistema.config.SunatProperties;
import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SunatEnvioService {

    static final String CODIGO_YA_REGISTRADO = "1033";

    private static final int LIMITE_TEXTO = 500;
    private static final int LIMITE_MASIVO = 40;
    private static final int FALLOS_SEGUIDOS_MAXIMOS = 3;
    private static final List<EstadoSunat> REENVIABLES = List.of(EstadoSunat.PENDIENTE, EstadoSunat.ERROR_ENVIO);

    private record Preparado(String ruc, String tipoCodigo, String serie, long correlativo, int intentos,
                             String nombreBase, byte[] xml, AmbienteSunat ambiente,
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
    private final SunatProperties propiedades;
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
            SunatGateway.RespuestaSunat original = gateway.enviarComprobante(
                    preparado.ambiente(), preparado.credenciales(), preparado.nombreBase(), preparado.xml());
            SunatGateway.RespuestaSunat respuesta = recuperar(preparado, original);
            boolean recuperada = respuesta != original;

            return new TransactionTemplate(transactionManager)
                    .execute(estado -> registrar(comprobanteId, actor, respuesta, recuperada));
        } finally {
            enCurso.remove(clave);
        }
    }

    public EnvioMasivoDTO enviarPendientes(Long sedeId, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        List<Long> ids = comprobanteRepository.findIdsParaEnviar(sedeId, REENVIABLES,
                PageRequest.of(0, LIMITE_MASIVO + 1));
        boolean quedanMas = ids.size() > LIMITE_MASIVO;

        return procesar(ids.subList(0, Math.min(LIMITE_MASIVO, ids.size())), actor, quedanMas);
    }

    public EnvioMasivoDTO enviarAutomatico(Usuario actor) {
        ConfiguracionSunat config = configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA).orElse(null);

        if (config == null || !config.isActiva()) {
            return new EnvioMasivoDTO(0, 0, 0, 0, false, null);
        }

        int maximo = Math.max(1, propiedades.getAutomaticoMaxPorCiclo());
        Instant ahora = Instant.now();
        List<Long> ids = new ArrayList<>();

        for (Object[] fila : archivoRepository.findCandidatosEnvio(REENVIABLES, PageRequest.of(0, maximo * 4))) {
            if (ReintentoEnvio.corresponde((Integer) fila[1], (Instant) fila[2], ahora,
                    propiedades.getAutomaticoMaxIntentos())) {
                ids.add((Long) fila[0]);
            }

            if (ids.size() > maximo) {
                break;
            }
        }

        boolean quedanMas = ids.size() > maximo;
        return procesar(ids.subList(0, Math.min(maximo, ids.size())), actor, quedanMas);
    }

    private EnvioMasivoDTO procesar(List<Long> ids, Usuario actor, boolean quedanMasInicial) {
        boolean quedanMas = quedanMasInicial;
        int aceptados = 0;
        int rechazados = 0;
        int conError = 0;
        int omitidos = 0;
        int seguidos = 0;
        String motivo = null;

        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                pausar();
            }

            try {
                EnvioResultadoDTO r = enviar(ids.get(i), actor);

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
            } catch (RuntimeException e) {
                omitidos++;
                seguidos = 0;

                if (motivo == null) {
                    motivo = "Ocurrió un error inesperado al enviar";
                }
            }

            if (seguidos >= FALLOS_SEGUIDOS_MAXIMOS) {
                quedanMas = true;
                break;
            }
        }

        return new EnvioMasivoDTO(aceptados, rechazados, conError, omitidos, quedanMas, motivo);
    }

    private void pausar() {
        long milisegundos = propiedades.getEnvioPausaMs();

        if (milisegundos <= 0) {
            return;
        }

        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SunatGateway.RespuestaSunat recuperar(Preparado p, SunatGateway.RespuestaSunat r) {
        if (r.estado() != EstadoSunat.ERROR_ENVIO || !CODIGO_YA_REGISTRADO.equals(r.codigo())) {
            return r;
        }

        if (p.intentos() < 1) {
            return new SunatGateway.RespuestaSunat(EstadoSunat.ERROR_ENVIO, r.codigo(),
                    "SUNAT ya tiene un comprobante con este número y datos distintos. Revisa la numeración antes de reenviar",
                    null);
        }

        SunatGateway.RespuestaSunat consulta = gateway.consultarConstancia(
                p.ambiente(), p.credenciales(), p.ruc(), p.tipoCodigo(), p.serie(), p.correlativo());

        if (consulta.cdrZip() != null && consulta.estado() != EstadoSunat.ERROR_ENVIO) {
            return consulta;
        }

        String motivo = consulta.mensaje() == null ? "" : " " + consulta.mensaje();
        return new SunatGateway.RespuestaSunat(EstadoSunat.ERROR_ENVIO, r.codigo(),
                "SUNAT ya tiene este comprobante, pero no se pudo traer su constancia." + motivo, null);
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

        return new Preparado(config.getRuc(), c.getTipo() == TipoComprobante.FACTURA ? "01" : "03",
                c.getSerie(), c.getCorrelativo(), archivo.getIntentos() == null ? 0 : archivo.getIntentos(),
                generador.nombreBase(c), archivo.getXml(), c.getAmbiente(), credenciales(config));
    }

    private SunatGateway.Credenciales credenciales(ConfiguracionSunat config) {
        if (config.getAmbiente() == AmbienteSunat.BETA) {
            return new SunatGateway.Credenciales(config.getRuc() + "MODDATOS", "MODDATOS");
        }

        return new SunatGateway.Credenciales(config.getRuc() + config.getUsuarioSol(),
                cifradoService.descifrarTexto(config.getClaveSolCifrada()));
    }

    private EnvioResultadoDTO registrar(Long comprobanteId, Usuario actor, SunatGateway.RespuestaSunat r,
                                        boolean recuperada) {
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
                        + ";intento=" + a.getIntentos() + ";recuperada=" + recuperada,
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