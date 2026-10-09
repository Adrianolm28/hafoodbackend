package com.hafood.sistema.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirmaDigitalServiceTest {

    private static final String CLAVE = "changeit";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\""
            + " xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\""
            + " xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
            + "<ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent/></ext:UBLExtension></ext:UBLExtensions>"
            + "</Invoice>";

    @TempDir
    Path carpeta;

    private final FirmaDigitalService servicio = new FirmaDigitalService(null, null);

    @Test
    void firmaYLaFirmaEsValida() throws Exception {
        FirmaDigitalService.Credencial credencial = crearCredencial();
        FirmaDigitalService.Firmado firmado = servicio.firmar(XML.getBytes(StandardCharsets.UTF_8), credencial);
        String texto = new String(firmado.xml(), StandardCharsets.UTF_8);

        assertTrue(texto.contains("Id=\"SIGN-HAFOOD\""));
        assertFalse(firmado.digest().isBlank());
        assertTrue(validar(firmado.xml(), credencial));
    }

    @Test
    void siSeAlteraElXmlLaFirmaFalla() throws Exception {
        FirmaDigitalService.Credencial credencial = crearCredencial();
        FirmaDigitalService.Firmado firmado = servicio.firmar(XML.getBytes(StandardCharsets.UTF_8), credencial);
        String alterado = new String(firmado.xml(), StandardCharsets.UTF_8).replace("</Invoice>", "<Alterado/></Invoice>");

        assertFalse(validar(alterado.getBytes(StandardCharsets.UTF_8), credencial));
    }

    private boolean validar(byte[] xml, FirmaDigitalService.Credencial credencial) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        Document documento = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList firmas = documento.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, firmas.getLength());
        DOMValidateContext contexto = new DOMValidateContext(credencial.certificado().getPublicKey(), firmas.item(0));
        return XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto).validate(contexto);
    }

    private FirmaDigitalService.Credencial crearCredencial() throws Exception {
        Path archivo = carpeta.resolve("prueba.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", "prueba", "-keyalg", "RSA",
                "-keysize", "2048", "-storetype", "PKCS12", "-keystore", archivo.toString(),
                "-storepass", CLAVE, "-keypass", CLAVE, "-dname", "CN=Prueba Hafood", "-validity", "30")
                .redirectErrorStream(true).start();
        proceso.getInputStream().readAllBytes();
        assertEquals(0, proceso.waitFor());

        KeyStore almacen = KeyStore.getInstance("PKCS12");

        try (InputStream entrada = Files.newInputStream(archivo)) {
            almacen.load(entrada, CLAVE.toCharArray());
        }

        return new FirmaDigitalService.Credencial(
                (PrivateKey) almacen.getKey("prueba", CLAVE.toCharArray()),
                (X509Certificate) almacen.getCertificate("prueba"));
    }
}