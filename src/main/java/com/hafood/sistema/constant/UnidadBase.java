package com.hafood.sistema.constant;

public enum UnidadBase {
    ML("ml"),
    G("g"),
    UNIDAD("unidad");

    private final String etiqueta;

    UnidadBase(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}