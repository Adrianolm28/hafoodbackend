package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoSunat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SunatGatewayDirectoTest {

    private final SunatGatewayDirecto gateway = new SunatGatewayDirecto();

    @Test
    void aceptado() throws Exception {
        SunatGateway.RespuestaSunat r = gateway.interpretar(soap(cdr("0", "Comprobante aceptado", null)));

        assertEquals(EstadoSunat.ACEPTADO, r.estado());
        assertEquals("0", r.codigo());
        assertNotNull(r.cdrZip());
    }

    @Test
    void aceptadoConObservaciones() throws Exception {
        SunatGateway.RespuestaSunat r = gateway.interpretar(soap(cdr("0", "Comprobante aceptado", "4000 - Observacion")));

        assertEquals(EstadoSunat.ACEPTADO_CON_OBSERVACIONES, r.estado());
    }

    @Test
    void rechazoPorFallo() {
        SunatGateway.RespuestaSunat r = gateway.interpretar(fallo("soap-env:Client.2335", "Documento rechazado"));

        assertEquals(EstadoSunat.RECHAZADO, r.estado());
        assertEquals("2335", r.codigo());
    }

    @Test
    void falloTransitorioSePuedeReintentar() {
        SunatGateway.RespuestaSunat r = gateway.interpretar(fallo("soap-env:Client.0111", "No tiene perfil"));

        assertEquals(EstadoSunat.ERROR_ENVIO, r.estado());
        assertEquals("0111", r.codigo());
    }

    @Test
    void respuestaIlegibleNoRompe() {
        assertEquals(EstadoSunat.ERROR_ENVIO, gateway.interpretar("esto no es xml").estado());
    }

    private String cdr(String codigo, String descripcion, String nota) {
        return "<ar:ApplicationResponse xmlns:ar=\"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2\""
                + " xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\""
                + " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">"
                + (nota == null ? "" : "<cbc:Note>" + nota + "</cbc:Note>")
                + "<cac:DocumentResponse><cac:Response><cbc:ResponseCode>" + codigo + "</cbc:ResponseCode>"
                + "<cbc:Description>" + descripcion + "</cbc:Description></cac:Response></cac:DocumentResponse>"
                + "</ar:ApplicationResponse>";
    }

    private String soap(String cdrXml) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("R-20000000001-01-F001-00000001.xml"));
            zip.write(cdrXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return "<soap-env:Envelope xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap-env:Body>"
                + "<br:sendBillResponse xmlns:br=\"http://service.sunat.gob.pe\"><applicationResponse>"
                + Base64.getEncoder().encodeToString(salida.toByteArray())
                + "</applicationResponse></br:sendBillResponse></soap-env:Body></soap-env:Envelope>";
    }

    private String fallo(String codigo, String texto) {
        return "<soap-env:Envelope xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap-env:Body>"
                + "<soap-env:Fault><faultcode>" + codigo + "</faultcode><faultstring>" + texto
                + "</faultstring></soap-env:Fault></soap-env:Body></soap-env:Envelope>";
    }
}