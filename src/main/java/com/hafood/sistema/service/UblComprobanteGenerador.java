package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteCliente;
import com.hafood.sistema.domain.sunat.ComprobanteLinea;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UblComprobanteGenerador {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final String MONEDA = "PEN";
    private static final String ID_FIRMA = "SIGN-HAFOOD";

    public String numero(Comprobante c) {
        return c.getSerie() + "-" + String.format("%08d", c.getCorrelativo());
    }

    public String nombreBase(Comprobante c) {
        return c.getRucEmisor() + "-" + codigoTipo(c.getTipo()) + "-" + numero(c);
    }

    public String nombreArchivo(Comprobante c) {
        return nombreBase(c) + ".xml";
    }

    public byte[] generar(Comprobante c, List<ComprobanteLinea> lineas) {
        validar(c, lineas);

        BigDecimal impuesto = TributoUbl.monto(c.getIgv(), c.getIpm());
        BigDecimal tasa = TributoUbl.tasa(c.getIgvPorcentaje(), c.getIpmPorcentaje());
        BigDecimal factor = BigDecimal.ONE.add(tasa.divide(CIEN, 6, RoundingMode.HALF_UP));
        Salida x = new Salida();

        x.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.raw("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\""
                + " xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\""
                + " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\""
                + " xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\""
                + " xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\">");
        x.abrir("ext:UBLExtensions").abrir("ext:UBLExtension").raw("<ext:ExtensionContent/>")
                .cerrar("ext:UBLExtension").cerrar("ext:UBLExtensions");
        x.elem("cbc:UBLVersionID", "2.1");
        x.elem("cbc:CustomizationID", "2.0");
        x.elem("cbc:ID", numero(c));
        x.elem("cbc:IssueDate", c.getFechaEmision().toString());
        x.elem("cbc:IssueTime", c.getEmitidoEn().atZone(ZONA).toLocalTime().format(HORA));
        x.elem("cbc:InvoiceTypeCode",
                " listID=\"0101\" listAgencyName=\"PE:SUNAT\" listName=\"Tipo de Documento\""
                        + " listURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01\"",
                codigoTipo(c.getTipo()));
        x.elem("cbc:Note", " languageLocaleID=\"1000\"", ImporteEnLetras.convertir(c.getTotal()));
        x.elem("cbc:DocumentCurrencyCode", MONEDA);

        firma(x, c);
        emisor(x, c);
        cliente(x, c);

        if (c.getTipo() == TipoComprobante.FACTURA) {
            x.abrir("cac:PaymentTerms").elem("cbc:ID", "FormaPago").elem("cbc:PaymentMeansID", "Contado")
                    .cerrar("cac:PaymentTerms");
        }

        x.abrir("cac:TaxTotal").monto("cbc:TaxAmount", impuesto);
        x.abrir("cac:TaxSubtotal").monto("cbc:TaxableAmount", c.getOpGravada()).monto("cbc:TaxAmount", impuesto);
        x.abrir("cac:TaxCategory").abrir("cac:TaxScheme").elem("cbc:ID", "1000").elem("cbc:Name", "IGV")
                .elem("cbc:TaxTypeCode", "VAT").cerrar("cac:TaxScheme").cerrar("cac:TaxCategory");
        x.cerrar("cac:TaxSubtotal").cerrar("cac:TaxTotal");

        x.abrir("cac:LegalMonetaryTotal").monto("cbc:LineExtensionAmount", c.getOpGravada())
                .monto("cbc:TaxInclusiveAmount", c.getTotal()).monto("cbc:PayableAmount", c.getTotal())
                .cerrar("cac:LegalMonetaryTotal");

        for (ComprobanteLinea linea : lineas) {
            linea(x, linea, tasa, factor);
        }

        x.raw("</Invoice>");
        return x.bytes();
    }

    private void validar(Comprobante c, List<ComprobanteLinea> lineas) {
        boolean factura = c.getTipo() == TipoComprobante.FACTURA;
        String patron = factura ? "F[A-Z0-9]{3}" : "B[A-Z0-9]{3}";

        if (c.getSerie() == null || !c.getSerie().matches(patron)) {
            throw new IllegalStateException("La serie " + c.getSerie() + " no sirve para "
                    + (factura ? "factura" : "boleta") + ". Debe tener 4 caracteres y empezar con "
                    + (factura ? "F" : "B"));
        }
        if (c.getRucEmisor() == null || c.getRucEmisor().length() != 11) {
            throw new IllegalStateException("El RUC del emisor no es válido");
        }
        if (c.getUbigeo() == null || c.getUbigeo().length() != 6) {
            throw new IllegalStateException("El ubigeo de la sede debe tener 6 dígitos");
        }
        if (c.getCodigoEstablecimiento() == null || c.getCodigoEstablecimiento().length() != 4) {
            throw new IllegalStateException("El código de establecimiento debe tener 4 dígitos");
        }
        if (lineas.isEmpty()) {
            throw new IllegalStateException("El comprobante no tiene productos");
        }

        BigDecimal base = BigDecimal.ZERO;
        BigDecimal impuestos = BigDecimal.ZERO;

        for (ComprobanteLinea l : lineas) {
            base = base.add(l.getBaseImponible());
            impuestos = impuestos.add(l.getIgv()).add(l.getIpm());
        }

        if (base.compareTo(c.getOpGravada()) != 0
                || impuestos.compareTo(c.getIgv().add(c.getIpm())) != 0
                || base.add(impuestos).compareTo(c.getTotal()) != 0) {
            throw new IllegalStateException("Los importes de las líneas no cuadran con el comprobante");
        }
    }

    private void firma(Salida x, Comprobante c) {
        x.abrir("cac:Signature").elem("cbc:ID", c.getRucEmisor());
        x.abrir("cac:SignatoryParty").abrir("cac:PartyIdentification").elem("cbc:ID", c.getRucEmisor())
                .cerrar("cac:PartyIdentification");
        x.abrir("cac:PartyName").elem("cbc:Name", c.getRazonSocialEmisor()).cerrar("cac:PartyName")
                .cerrar("cac:SignatoryParty");
        x.abrir("cac:DigitalSignatureAttachment").abrir("cac:ExternalReference").elem("cbc:URI", "#" + ID_FIRMA)
                .cerrar("cac:ExternalReference").cerrar("cac:DigitalSignatureAttachment");
        x.cerrar("cac:Signature");
    }

    private void emisor(Salida x, Comprobante c) {
        x.abrir("cac:AccountingSupplierParty").abrir("cac:Party");
        x.abrir("cac:PartyIdentification").elem("cbc:ID", " schemeID=\"6\"", c.getRucEmisor())
                .cerrar("cac:PartyIdentification");

        if (!vacio(c.getNombreComercialEmisor())) {
            x.abrir("cac:PartyName").elem("cbc:Name", c.getNombreComercialEmisor()).cerrar("cac:PartyName");
        }

        x.abrir("cac:PartyLegalEntity").elem("cbc:RegistrationName", c.getRazonSocialEmisor());
        x.abrir("cac:RegistrationAddress").elem("cbc:ID", c.getUbigeo());
        x.elem("cbc:AddressTypeCode", " listAgencyName=\"PE:SUNAT\" listName=\"Establecimientos anexos\"",
                c.getCodigoEstablecimiento());
        x.abrir("cac:AddressLine").elem("cbc:Line", c.getDireccionFiscal()).cerrar("cac:AddressLine");
        x.abrir("cac:Country").elem("cbc:IdentificationCode", "PE").cerrar("cac:Country");
        x.cerrar("cac:RegistrationAddress").cerrar("cac:PartyLegalEntity").cerrar("cac:Party")
                .cerrar("cac:AccountingSupplierParty");
    }

    private void cliente(Salida x, Comprobante c) {
        ComprobanteCliente cl = c.getCliente();
        boolean sinDocumento = cl.getTipoDocumento() == TipoDocumentoCliente.SIN_DOCUMENTO
                || vacio(cl.getNumeroDocumento());

        x.abrir("cac:AccountingCustomerParty").abrir("cac:Party");
        x.abrir("cac:PartyIdentification").elem("cbc:ID",
                " schemeID=\"" + codigoDocumento(cl.getTipoDocumento()) + "\"",
                sinDocumento ? "-" : cl.getNumeroDocumento()).cerrar("cac:PartyIdentification");
        x.abrir("cac:PartyLegalEntity").elem("cbc:RegistrationName", cl.getNombre());

        if (!vacio(cl.getDireccion())) {
            x.abrir("cac:RegistrationAddress").abrir("cac:AddressLine").elem("cbc:Line", cl.getDireccion())
                    .cerrar("cac:AddressLine").cerrar("cac:RegistrationAddress");
        }

        x.cerrar("cac:PartyLegalEntity").cerrar("cac:Party").cerrar("cac:AccountingCustomerParty");
    }

    private void linea(Salida x, ComprobanteLinea l, BigDecimal tasa, BigDecimal factor) {
        BigDecimal impuesto = TributoUbl.monto(l.getIgv(), l.getIpm());
        BigDecimal valorUnitario = l.getPrecioUnitario().divide(factor, 10, RoundingMode.HALF_UP);

        x.abrir("cac:InvoiceLine");
        x.elem("cbc:ID", String.valueOf(l.getItem()));
        x.elem("cbc:InvoicedQuantity", " unitCode=\"" + l.getUnidadMedida() + "\"", String.valueOf(l.getCantidad()));
        x.monto("cbc:LineExtensionAmount", l.getBaseImponible());
        x.abrir("cac:PricingReference").abrir("cac:AlternativeConditionPrice")
                .monto("cbc:PriceAmount", l.getPrecioUnitario()).elem("cbc:PriceTypeCode", "01")
                .cerrar("cac:AlternativeConditionPrice").cerrar("cac:PricingReference");

        if (l.getDescuento().signum() > 0) {
            BigDecimal descuentoBase = l.getDescuento().divide(factor, 2, RoundingMode.HALF_UP);
            BigDecimal brutoBase = l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad()))
                    .divide(factor, 2, RoundingMode.HALF_UP);

            x.abrir("cac:AllowanceCharge").elem("cbc:ChargeIndicator", "false")
                    .elem("cbc:AllowanceChargeReasonCode", "00").monto("cbc:Amount", descuentoBase)
                    .monto("cbc:BaseAmount", brutoBase).cerrar("cac:AllowanceCharge");
        }

        x.abrir("cac:TaxTotal").monto("cbc:TaxAmount", impuesto);
        x.abrir("cac:TaxSubtotal").monto("cbc:TaxableAmount", l.getBaseImponible()).monto("cbc:TaxAmount", impuesto);
        x.abrir("cac:TaxCategory").elem("cbc:Percent", tasa.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .elem("cbc:TaxExemptionReasonCode", "10");
        x.abrir("cac:TaxScheme").elem("cbc:ID", "1000").elem("cbc:Name", "IGV").elem("cbc:TaxTypeCode", "VAT")
                .cerrar("cac:TaxScheme").cerrar("cac:TaxCategory").cerrar("cac:TaxSubtotal").cerrar("cac:TaxTotal");

        x.abrir("cac:Item").elem("cbc:Description", l.getDescripcion()).cerrar("cac:Item");
        x.abrir("cac:Price").precio("cbc:PriceAmount", valorUnitario).cerrar("cac:Price");
        x.cerrar("cac:InvoiceLine");
    }

    private String codigoTipo(TipoComprobante tipo) {
        return tipo == TipoComprobante.FACTURA ? "01" : "03";
    }

    private String codigoDocumento(TipoDocumentoCliente tipo) {
        return switch (tipo) {
            case SIN_DOCUMENTO -> "0";
            case DNI -> "1";
            case CARNET_EXTRANJERIA -> "4";
            case RUC -> "6";
            case PASAPORTE -> "7";
        };
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String escapar(String valor) {
        StringBuilder sb = new StringBuilder(valor.length() + 8);

        for (char ch : valor.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        sb.append(ch);
                    }
                }
            }
        }

        return sb.toString();
    }

    private static final class Salida {

        private final StringBuilder texto = new StringBuilder(4096);

        Salida raw(String linea) {
            texto.append(linea).append('\n');
            return this;
        }

        Salida abrir(String nombre) {
            return raw("<" + nombre + ">");
        }

        Salida cerrar(String nombre) {
            return raw("</" + nombre + ">");
        }

        Salida elem(String nombre, String valor) {
            return elem(nombre, "", valor);
        }

        Salida elem(String nombre, String atributos, String valor) {
            return raw("<" + nombre + atributos + ">" + escapar(valor) + "</" + nombre + ">");
        }

        Salida monto(String nombre, BigDecimal valor) {
            return elem(nombre, " currencyID=\"" + MONEDA + "\"",
                    valor.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }

        Salida precio(String nombre, BigDecimal valor) {
            BigDecimal v = valor.stripTrailingZeros();

            if (v.scale() < 2) {
                v = v.setScale(2);
            }

            return elem(nombre, " currencyID=\"" + MONEDA + "\"", v.toPlainString());
        }

        byte[] bytes() {
            return texto.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}