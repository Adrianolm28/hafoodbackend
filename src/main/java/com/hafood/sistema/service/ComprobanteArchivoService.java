package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteArchivo;
import com.hafood.sistema.domain.sunat.ComprobanteLinea;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.repository.ComprobanteArchivoRepository;
import com.hafood.sistema.repository.ComprobanteLineaRepository;
import com.hafood.sistema.repository.ComprobanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComprobanteArchivoService {

    private static final int LIMITE_ERROR = 300;

    public record Descarga(String nombre, byte[] contenido) {
    }

    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteLineaRepository comprobanteLineaRepository;
    private final ComprobanteArchivoRepository archivoRepository;
    private final UblComprobanteGenerador generador;
    private final FirmaDigitalService firmaService;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public void guardarXml(Comprobante comprobante, List<ComprobanteLinea> lineas) {
        ComprobanteArchivo archivo = obtenerOCrear(comprobante);
        armar(archivo, comprobante, lineas);
        archivoRepository.save(archivo);
    }

    @Transactional(readOnly = true)
    public Descarga descargarXml(Long comprobanteId, Usuario actor) {
        buscar(comprobanteId, actor);
        ComprobanteArchivo archivo = archivoRepository.findByComprobanteId(comprobanteId)
                .orElseThrow(() -> conflicto("El XML de este comprobante aún no está generado"));

        if (archivo.getXml() == null) {
            throw conflicto(archivo.getErrorGeneracion() == null
                    ? "El XML de este comprobante aún no está generado"
                    : "El XML no se pudo generar: " + archivo.getErrorGeneracion());
        }

        return new Descarga(archivo.getNombreArchivo(), archivo.getXml());
    }

    @Transactional
    public void regenerarXml(Long comprobanteId, Usuario actor) {
        Comprobante comprobante = buscar(comprobanteId, actor);

        if (comprobante.getEstadoSunat() != EstadoSunat.PENDIENTE) {
            throw conflicto("Solo se puede regenerar el XML de un comprobante pendiente de envío");
        }

        ComprobanteArchivo archivo = obtenerOCrear(comprobante);

        if (archivo.isFirmado()) {
            throw conflicto("El comprobante ya fue firmado y no se puede regenerar");
        }

        String error = armar(archivo, comprobante,
                comprobanteLineaRepository.findByComprobanteIdOrderByItem(comprobanteId));

        if (error != null) {
            throw conflicto("No se pudo preparar el comprobante: " + error);
        }

        archivoRepository.save(archivo);
        auditoriaService.registrar(actor, comprobante.getSede().getId(), comprobante.getCuenta().getId(),
                AccionAuditoria.COMPROBANTE_XML_REGENERADO, null,
                "numero=" + generador.numero(comprobante), null);
    }

    private String armar(ComprobanteArchivo archivo, Comprobante comprobante, List<ComprobanteLinea> lineas) {
        archivo.setFirmado(false);
        archivo.setHashFirma(null);
        archivo.setFirmadoEn(null);

        try {
            byte[] xml = generador.generar(comprobante, lineas);
            archivo.setXml(xml);
            archivo.setNombreArchivo(generador.nombreArchivo(comprobante));
            archivo.setXmlGeneradoEn(Instant.now());
            archivo.setErrorGeneracion(null);

            FirmaDigitalService.Firmado firmado = firmaService.firmar(xml);
            archivo.setXml(firmado.xml());
            archivo.setHashFirma(firmado.digest());
            archivo.setFirmado(true);
            archivo.setFirmadoEn(Instant.now());
            return null;
        } catch (RuntimeException e) {
            String mensaje = e instanceof IllegalStateException && e.getMessage() != null
                    ? e.getMessage()
                    : "Error inesperado al preparar el comprobante";
            String recortado = mensaje.length() <= LIMITE_ERROR ? mensaje : mensaje.substring(0, LIMITE_ERROR);
            archivo.setErrorGeneracion(recortado);
            return recortado;
        }
    }

    private ComprobanteArchivo obtenerOCrear(Comprobante comprobante) {
        return archivoRepository.findByComprobanteId(comprobante.getId()).orElseGet(() -> {
            ComprobanteArchivo nuevo = new ComprobanteArchivo();
            nuevo.setComprobante(comprobante);
            return nuevo;
        });
    }

    private Comprobante buscar(Long comprobanteId, Usuario actor) {
        Comprobante comprobante = comprobanteRepository.findById(comprobanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El comprobante no existe"));
        sedeAccesoService.exigirAcceso(actor, comprobante.getSede().getId());
        return comprobante;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}