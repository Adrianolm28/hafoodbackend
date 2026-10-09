package com.hafood.sistema.service;

import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
public class FirmaDigitalService {

    static final String ID_FIRMA = "SIGN-HAFOOD";

    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    public record Firmado(byte[] xml, String digest) {
    }

    record Credencial(PrivateKey llave, X509Certificate certificado) {
    }

    private final ConfiguracionSunatRepository configRepository;
    private final CifradoService cifradoService;

    public Firmado firmar(byte[] xml) {
        return firmar(xml, cargarCredencial());
    }

    Firmado firmar(byte[] xml, Credencial credencial) {
        try {
            credencial.certificado().checkValidity();
        } catch (CertificateExpiredException e) {
            throw new IllegalStateException("El certificado digital está vencido. Carga uno nuevo en Facturación");
        } catch (CertificateNotYetValidException e) {
            throw new IllegalStateException("El certificado digital todavía no es válido");
        }

        Document documento = parsear(xml);
        Node contenido = buscarContenidoFirma(documento);

        try {
            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fabrica.newReference("",
                    fabrica.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null, null);
            SignedInfo info = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod(RSA_SHA256, null),
                    List.of(referencia));
            KeyInfoFactory fabricaClave = fabrica.getKeyInfoFactory();
            KeyInfo claveInfo = fabricaClave.newKeyInfo(
                    List.of(fabricaClave.newX509Data(List.of(credencial.certificado()))));

            DOMSignContext contexto = new DOMSignContext(credencial.llave(), contenido);
            contexto.setDefaultNamespacePrefix("ds");
            fabrica.newXMLSignature(info, claveInfo, null, ID_FIRMA, null).sign(contexto);

            verificar(fabrica, documento, credencial.certificado());

            return new Firmado(serializar(documento), extraerDigest(documento));
        } catch (GeneralSecurityException | MarshalException | XMLSignatureException e) {
            throw new IllegalStateException("No se pudo firmar el comprobante con el certificado digital");
        }
    }

    private void verificar(XMLSignatureFactory fabrica, Document documento, X509Certificate certificado)
            throws MarshalException, XMLSignatureException {
        NodeList firmas = documento.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (firmas.getLength() != 1) {
            throw new IllegalStateException("La firma del comprobante no quedó bien armada");
        }

        DOMValidateContext contexto = new DOMValidateContext(certificado.getPublicKey(), firmas.item(0));

        if (!fabrica.unmarshalXMLSignature(contexto).validate(contexto)) {
            throw new IllegalStateException("La firma generada no pasó la verificación interna");
        }
    }

    private String extraerDigest(Document documento) {
        NodeList digests = documento.getElementsByTagNameNS(XMLSignature.XMLNS, "DigestValue");

        if (digests.getLength() < 1) {
            throw new IllegalStateException("La firma del comprobante no quedó bien armada");
        }

        return digests.item(0).getTextContent().trim();
    }

    private Node buscarContenidoFirma(Document documento) {
        NodeList nodos = documento.getElementsByTagNameNS(NS_EXT, "ExtensionContent");

        if (nodos.getLength() != 1) {
            throw new IllegalStateException("El XML no tiene el espacio para la firma");
        }

        return nodos.item(0);
    }

    private Document parsear(byte[] xml) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("El XML del comprobante no es válido");
        }
    }

    private byte[] serializar(Document documento) {
        try {
            documento.setXmlStandalone(true);
            TransformerFactory fabrica = TransformerFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformador = fabrica.newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            transformador.transform(new DOMSource(documento), new StreamResult(salida));
            return salida.toByteArray();
        } catch (TransformerException e) {
            throw new IllegalStateException("No se pudo guardar el XML firmado");
        }
    }

    private Credencial cargarCredencial() {
        ConfiguracionSunat config = configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA)
                .orElseThrow(() -> new IllegalStateException("La facturación electrónica no está configurada"));

        if (config.getCertificadoCifrado() == null || config.getCertificadoPasswordCifrada() == null) {
            throw new IllegalStateException("Falta cargar el certificado digital en Facturación");
        }

        char[] clave = cifradoService.descifrarTexto(config.getCertificadoPasswordCifrada()).toCharArray();

        try {
            byte[] pfx = cifradoService.descifrar(config.getCertificadoCifrado());
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(pfx), clave);
            Enumeration<String> alias = almacen.aliases();

            while (alias.hasMoreElements()) {
                String nombre = alias.nextElement();

                if (!almacen.isKeyEntry(nombre)) {
                    continue;
                }

                Key llave = almacen.getKey(nombre, clave);
                Certificate certificado = almacen.getCertificate(nombre);

                if (llave instanceof PrivateKey privada && "RSA".equals(llave.getAlgorithm())
                        && certificado instanceof X509Certificate x509) {
                    return new Credencial(privada, x509);
                }
            }

            throw new IllegalStateException("El certificado digital no tiene una llave RSA. Carga otro en Facturación");
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo abrir el certificado digital. Vuelve a cargarlo en Facturación");
        } finally {
            Arrays.fill(clave, '\0');
        }
    }
}