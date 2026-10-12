package com.hafood.sistema.service;

import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunatGatewayConsultaTest {

    private final SunatGatewayDirecto gateway = new SunatGatewayDirecto();

    private static String cdrBase64() throws IOException {
        String xml = "<ar:ApplicationResponse xmlns:ar=\"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2\""
                + " xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\""
                + " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">"
                + "<cac:DocumentResponse><cac:Response><cbc:ResponseCode>0</cbc:ResponseCode>"
                + "<cbc:Description>La Boleta numero B001-4, ha sido aceptada</cbc:Description>"
                + "</cac:Response></cac:DocumentResponse></ar:ApplicationResponse>";
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("R-20000000001-03-B001-00000004.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return Base64.getEncoder().encodeToString(salida.toByteArray());
    }

    private static String respuestaConsulta(String interior) {
        return "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body>"
                + "<ns2:getStatusCdrResponse xmlns:ns2=\"http://service.sunat.gob.pe\"><statusCdr>"
                + interior + "</statusCdr></ns2:getStatusCdrResponse></S:Body></S:Envelope>";
    }

    @Test
    void laFallaConservaElCodigoUnoCeroTresTres() {
        String falla = "<soap-env:Envelope xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap-env:Body>"
                + "<soap-env:Fault><faultcode>soap-env:Client.1033</faultcode>"
                + "<faultstring>El comprobante fue registrado previamente con otros datos</faultstring>"
                + "</soap-env:Fault></soap-env:Body></soap-env:Envelope>";

        SunatGateway.RespuestaSunat r = gateway.interpretar(falla);

        assertEquals(EstadoSunat.ERROR_ENVIO, r.estado());
        assertEquals("1033", r.codigo());
        assertNull(r.cdrZip());
    }

    @Test
    void laConsultaConConstanciaDevuelveAceptado() throws IOException {
        SunatGateway.RespuestaSunat r = gateway.interpretarConsulta(respuestaConsulta(
                "<content>" + cdrBase64() + "</content><statusCode>0001</statusCode>"
                        + "<statusMessage>El comprobante existe y esta aceptado.</statusMessage>"));

        assertEquals(EstadoSunat.ACEPTADO, r.estado());
        assertEquals("0", r.codigo());
        assertNotNull(r.cdrZip());
    }

    @Test
    void laConsultaSinConstanciaQuedaEnError() {
        SunatGateway.RespuestaSunat r = gateway.interpretarConsulta(respuestaConsulta(
                "<statusCode>0001</statusCode><statusMessage>El comprobante existe y esta aceptado.</statusMessage>"));

        assertEquals(EstadoSunat.ERROR_ENVIO, r.estado());
        assertEquals("0001", r.codigo());
        assertNull(r.cdrZip());
    }

    @Test
    void laConsultaEnBetaNoSalePorLaRed() {
        SunatGateway.RespuestaSunat r = gateway.consultarConstancia(AmbienteSunat.BETA,
                new SunatGateway.Credenciales("20000000001MODDATOS", "MODDATOS"),
                "20000000001", "03", "B001", 4L);

        assertEquals(EstadoSunat.ERROR_ENVIO, r.estado());
        assertTrue(r.mensaje().contains("producción"));
    }
}