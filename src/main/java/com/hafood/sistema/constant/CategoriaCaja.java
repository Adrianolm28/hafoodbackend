package com.hafood.sistema.constant;

public enum CategoriaCaja {
    PAGO_APOYO(false, true),
    COMPRA(false, true),
    ADELANTO_PERSONAL(false, true),
    SERVICIO(false, true),
    REPOSICION_FONDO(true, false),
    SOBRANTE(true, false),
    OTRO(true, true);

    private final boolean ingreso;
    private final boolean egreso;

    CategoriaCaja(boolean ingreso, boolean egreso) {
        this.ingreso = ingreso;
        this.egreso = egreso;
    }

    public boolean permiteIngreso() {
        return ingreso;
    }

    public boolean permiteEgreso() {
        return egreso;
    }
}