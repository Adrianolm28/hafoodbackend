package com.hafood.sistema.service;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.DocumentBuilder;

@Component
public class SunatGatewayDirecto implements SunatGateway {

    private static final String NS_SERVICIO = "http://service.sunat.gob.pe";
    private static final String NS_WSSE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final int MAX_CDR_BYTES = 2_000_000;
    private static final int LIMITE_MENSAJE = 400;
    private static final Pattern CODIGO = Pattern.compile("(\\d{3,4})\\D*$");

    private final RestClient cliente;

    public SunatGatewayDirecto() {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
        fabrica.setReadTimeout(Duration.ofSeconds(60));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public RespuestaSunat enviarComprobante(AmbienteSunat ambiente, Credenciales credenciales,
                                            String nombreBase, byte[] xmlFirmado) {
        String zip = Base64.getEncoder().encodeToString(comprimir(nombreBase + ".xml", xmlFirmado));
        String sobre = sobre(credenciales, nombreBase + ".zip", zip);
        record Crudo(int estado, String tipo, String cuerpo) {
        }

        Crudo crudo;

        try {
            crudo = cliente.post()
                    .uri(ambiente.getEndpoint())
                    .contentType(MediaType.parseMediaType("text/xml; charset=UTF-8"))
                    .header("SOAPAction", "")
                    .body(sobre)
                    .exchange((peticion, respuesta) -> new Crudo(
                            respuesta.getStatusCode().value(),
                            String.valueOf(respuesta.getHeaders().getContentType()),
                            respuesta.bodyTo(String.class)));
        } catch (RestClientException e) {
            String causa = e.getCause() == null ? "" : "/" + e.getCause().getClass().getSimpleName();
            return error("No se pudo conectar con SUNAT (" + e.getClass().getSimpleName() + causa
                    + "). Se reintentará más tarde");
        }

        String detalle = " [HTTP " + crudo.estado() + ", " + crudo.tipo() + "]";

        if (crudo.cuerpo() == null || crudo.cuerpo().isBlank()) {
            return error("SUNAT no devolvió contenido" + detalle);
        }

        RespuestaSunat respuesta = interpretar(crudo.cuerpo());

        if (respuesta.estado() == EstadoSunat.ERROR_ENVIO && respuesta.cdrZip() == null) {
            return error(respuesta.mensaje() + detalle);
        }

        return respuesta;
    }

    RespuestaSunat interpretar(String cuerpo) {
        Document documento;

        try {
            documento = parsear(cuerpo.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return error("SUNAT devolvió una respuesta que no se pudo leer");
        }

        String fallo = texto(documento, "faultcode");

        if (fallo != null) {
            String detalle = recortar(texto(documento, "faultstring"));
            String codigo = codigoDe(fallo);
            return new RespuestaSunat(esRechazo(codigo) ? EstadoSunat.RECHAZADO : EstadoSunat.ERROR_ENVIO, codigo,
                    detalle == null ? "SUNAT no aceptó el comprobante" : detalle, null);
        }

        String contenido = texto(documento, "applicationResponse");

        if (contenido == null) {
            return error("SUNAT respondió sin la constancia de recepción");
        }

        byte[] zip;

        try {
            zip = Base64.getMimeDecoder().decode(contenido);
        } catch (IllegalArgumentException e) {
            return error("La constancia de SUNAT no se pudo leer");
        }

        return desdeCdr(zip);
    }

    private RespuestaSunat desdeCdr(byte[] zip) {
        Document cdr;

        try {
            cdr = parsear(leerXml(zip));
        } catch (Exception e) {
            return new RespuestaSunat(EstadoSunat.ERROR_ENVIO, null,
                    "La constancia de SUNAT no se pudo leer", zip);
        }

        String codigo = texto(cdr, "ResponseCode");
        String descripcion = texto(cdr, "Description");
        List<String> notas = notas(cdr);
        String mensaje = descripcion == null ? "" : descripcion;

        if (!notas.isEmpty()) {
            mensaje = mensaje + (mensaje.isEmpty() ? "" : " | ") + String.join(" | ", notas);
        }

        EstadoSunat estado;

        if ("0".equals(codigo)) {
            estado = notas.isEmpty() ? EstadoSunat.ACEPTADO : EstadoSunat.ACEPTADO_CON_OBSERVACIONES;
        } else if (esRechazo(codigo)) {
            estado = EstadoSunat.RECHAZADO;
        } else {
            estado = EstadoSunat.ERROR_ENVIO;
        }

        return new RespuestaSunat(estado, codigo, recortar(mensaje), zip);
    }

    private List<String> notas(Document cdr) {
        List<String> notas = new ArrayList<>();
        NodeList hijos = cdr.getDocumentElement().getChildNodes();

        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);

            if ("Note".equals(hijo.getLocalName()) && !hijo.getTextContent().isBlank()) {
                notas.add(hijo.getTextContent().trim());
            }
        }

        return notas;
    }

    private boolean esRechazo(String codigo) {
        if (codigo == null || !codigo.matches("\\d{1,4}")) {
            return false;
        }

        int numero = Integer.parseInt(codigo);
        return numero >= 2000 && numero <= 3999;
    }

    private String codigoDe(String texto) {
        Matcher m = CODIGO.matcher(texto.trim());
        return m.find() ? m.group(1) : null;
    }

    private RespuestaSunat error(String mensaje) {
        return new RespuestaSunat(EstadoSunat.ERROR_ENVIO, null, mensaje, null);
    }

    private String sobre(Credenciales c, String nombreZip, String contenido) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:ser=\"" + NS_SERVICIO + "\" xmlns:wsse=\"" + NS_WSSE + "\">"
                + "<soapenv:Header><wsse:Security><wsse:UsernameToken>"
                + "<wsse:Username>" + escapar(c.usuario()) + "</wsse:Username>"
                + "<wsse:Password>" + escapar(c.clave()) + "</wsse:Password>"
                + "</wsse:UsernameToken></wsse:Security></soapenv:Header>"
                + "<soapenv:Body><ser:sendBill><fileName>" + escapar(nombreZip) + "</fileName>"
                + "<contentFile>" + contenido + "</contentFile></ser:sendBill></soapenv:Body></soapenv:Envelope>";
    }

    private byte[] comprimir(String nombre, byte[] contenido) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();

            try (ZipOutputStream zip = new ZipOutputStream(salida)) {
                zip.putNextEntry(new ZipEntry(nombre));
                zip.write(contenido);
                zip.closeEntry();
            }

            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar el archivo para SUNAT");
        }
    }

    private byte[] leerXml(byte[] zip) throws IOException {
        try (ZipInputStream entrada = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry item;

            while ((item = entrada.getNextEntry()) != null) {
                if (!item.isDirectory() && item.getName().toLowerCase().endsWith(".xml")) {
                    byte[] datos = entrada.readNBytes(MAX_CDR_BYTES + 1);

                    if (datos.length > MAX_CDR_BYTES) {
                        throw new IOException("Constancia demasiado grande");
                    }

                    return datos;
                }
            }
        }

        throw new IOException("La constancia no trae XML");
    }

    private Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        DocumentBuilder constructor = fabrica.newDocumentBuilder();
        constructor.setErrorHandler(new DefaultHandler());
        return constructor.parse(new ByteArrayInputStream(xml));
    }

    private String texto(Document documento, String nombreLocal) {
        NodeList nodos = documento.getElementsByTagNameNS("*", nombreLocal);

        if (nodos.getLength() == 0) {
            return null;
        }

        String valor = nodos.item(0).getTextContent();
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }

        return texto.length() <= LIMITE_MENSAJE ? texto : texto.substring(0, LIMITE_MENSAJE);
    }

    private String escapar(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}