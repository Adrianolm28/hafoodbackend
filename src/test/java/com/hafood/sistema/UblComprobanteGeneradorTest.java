package com.hafood.sistema;

import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteCliente;
import com.hafood.sistema.domain.sunat.ComprobanteLinea;

import com.hafood.sistema.service.ImporteEnLetras;
import com.hafood.sistema.service.UblComprobanteGenerador;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UblComprobanteGeneradorTest {

    private final UblComprobanteGenerador generador = new UblComprobanteGenerador();

    @Test
    void importesEnLetras() {
        assertEquals("CUARENTA CON 00/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("40.00")));
        assertEquals("CIENTO DIECIOCHO CON 50/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("118.50")));
        assertEquals("CIEN CON 00/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("100.00")));
        assertEquals("MIL VEINTIUNO CON 05/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("1021.05")));
        assertEquals("VEINTIUN MIL CON 00/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("21000.00")));
        assertEquals("UN MILLON CON 00/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("1000000.00")));
        assertEquals("CERO CON 50/100 SOLES", ImporteEnLetras.convertir(new BigDecimal("0.50")));
    }

    @Test
    void generaBoletaBienFormada() throws Exception {
        String xml = new String(generador.generar(comprobante(TipoComprobante.BOLETA, "B001"), List.of(linea())),
                StandardCharsets.UTF_8);

        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document documento = fabrica.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("Invoice", documento.getDocumentElement().getLocalName());
        assertTrue(xml.contains("<cbc:ID>B001-00000001</cbc:ID>"));
        assertTrue(xml.contains("03</cbc:InvoiceTypeCode>"));
        assertTrue(xml.contains("CUARENTA CON 00/100 SOLES"));
        assertTrue(xml.contains("<cbc:ID schemeID=\"0\">-</cbc:ID>"));
        assertTrue(xml.contains("<cbc:PayableAmount currencyID=\"PEN\">40.00</cbc:PayableAmount>"));
        assertEquals("10728657265-03-B001-00000001.xml",
                generador.nombreArchivo(comprobante(TipoComprobante.BOLETA, "B001")));
    }

    @Test
    void rechazaSerieQueNoCorrespondeAlTipo() {
        assertThrows(IllegalStateException.class,
                () -> generador.generar(comprobante(TipoComprobante.FACTURA, "B001"), List.of(linea())));
    }

    private Comprobante comprobante(TipoComprobante tipo, String serie) {
        return Comprobante.builder()
                .tipo(tipo)
                .serie(serie)
                .correlativo(1)
                .fechaEmision(LocalDate.of(2026, 10, 9))
                .emitidoEn(Instant.parse("2026-10-09T17:34:00Z"))
                .rucEmisor("10728657265")
                .razonSocialEmisor("EMPRESA DE PRUEBA SAC")
                .codigoEstablecimiento("0000")
                .direccionFiscal("AV. PRUEBA 123")
                .ubigeo("150140")
                .igvPorcentaje(new BigDecimal("18.00"))
                .ipmPorcentaje(new BigDecimal("0.00"))
                .cliente(ComprobanteCliente.builder()
                        .tipoDocumento(TipoDocumentoCliente.SIN_DOCUMENTO)
                        .nombre("CLIENTES VARIOS")
                        .build())
                .opGravada(new BigDecimal("33.90"))
                .igv(new BigDecimal("6.10"))
                .ipm(new BigDecimal("0.00"))
                .total(new BigDecimal("40.00"))
                .build();
    }

    private ComprobanteLinea linea() {
        return ComprobanteLinea.builder()
                .item(1)
                .descripcion("Chilcano Clasico")
                .unidadMedida("NIU")
                .cantidad(1)
                .precioUnitario(new BigDecimal("40.00"))
                .descuento(new BigDecimal("0.00"))
                .total(new BigDecimal("40.00"))
                .baseImponible(new BigDecimal("33.90"))
                .igv(new BigDecimal("6.10"))
                .ipm(new BigDecimal("0.00"))
                .build();
    }
}