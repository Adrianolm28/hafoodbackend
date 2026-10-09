package com.hafood.sistema.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ImporteEnLetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO",
            "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO",
            "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO",
            "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
            "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private ImporteEnLetras() {
    }

    public static String convertir(BigDecimal importe) {
        if (importe == null || importe.signum() < 0) {
            throw new IllegalArgumentException("El importe no es válido");
        }

        long centavosTotales = importe.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        long soles = centavosTotales / 100;
        long centavos = centavosTotales % 100;

        if (soles >= 1_000_000_000L) {
            throw new IllegalArgumentException("El importe es demasiado grande");
        }

        return enLetras(soles) + " CON " + String.format("%02d", centavos) + "/100 SOLES";
    }

    private static String enLetras(long n) {
        if (n == 0) {
            return "CERO";
        }

        int millones = (int) (n / 1_000_000);
        int miles = (int) ((n % 1_000_000) / 1000);
        int resto = (int) (n % 1000);
        List<String> partes = new ArrayList<>();

        if (millones > 0) {
            partes.add(millones == 1 ? "UN MILLON" : apocopar(centenas(millones)) + " MILLONES");
        }
        if (miles > 0) {
            partes.add(miles == 1 ? "MIL" : apocopar(centenas(miles)) + " MIL");
        }
        if (resto > 0) {
            partes.add(centenas(resto));
        }

        return String.join(" ", partes);
    }

    private static String centenas(int n) {
        if (n == 100) {
            return "CIEN";
        }

        StringBuilder texto = new StringBuilder();
        int c = n / 100;
        int r = n % 100;

        if (c > 0) {
            texto.append(CENTENAS[c]);
        }
        if (r > 0) {
            if (texto.length() > 0) {
                texto.append(' ');
            }
            if (r < 30) {
                texto.append(UNIDADES[r]);
            } else {
                texto.append(DECENAS[r / 10]);
                if (r % 10 > 0) {
                    texto.append(" Y ").append(UNIDADES[r % 10]);
                }
            }
        }

        return texto.toString();
    }

    private static String apocopar(String texto) {
        return texto.endsWith("UNO") ? texto.substring(0, texto.length() - 1) : texto;
    }
}