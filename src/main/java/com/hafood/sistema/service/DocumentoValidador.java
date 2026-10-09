package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoDocumentoCliente;

import java.util.regex.Pattern;

public final class DocumentoValidador {

    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern CARNET = Pattern.compile("[A-Za-z0-9]{9,12}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Za-z0-9]{5,12}");
    private static final Pattern RUC = Pattern.compile("(10|15|16|17|20)\\d{9}");
    private static final int[] PESOS_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private DocumentoValidador() {
    }

    public static boolean esValido(TipoDocumentoCliente tipo, String numero) {
        boolean vacio = numero == null || numero.isBlank();

        return switch (tipo) {
            case SIN_DOCUMENTO -> vacio;
            case DNI -> !vacio && DNI.matcher(numero).matches();
            case CARNET_EXTRANJERIA -> !vacio && CARNET.matcher(numero).matches();
            case PASAPORTE -> !vacio && PASAPORTE.matcher(numero).matches();
            case RUC -> !vacio && rucValido(numero);
        };
    }

    private static boolean rucValido(String ruc) {
        if (!RUC.matcher(ruc).matches()) {
            return false;
        }

        int suma = 0;

        for (int i = 0; i < PESOS_RUC.length; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS_RUC[i];
        }

        int digito = (11 - (suma % 11)) % 10;
        return digito == ruc.charAt(10) - '0';
    }
}