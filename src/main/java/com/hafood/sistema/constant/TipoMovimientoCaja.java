package com.hafood.sistema.constant;

public enum TipoMovimientoCaja {
    APERTURA(1),
    INGRESO(1),
    COBRO(1),
    PROPINA(1),
    EGRESO(-1),
    DEVOLUCION(-1);

    private final int signo;

    TipoMovimientoCaja(int signo) {
        this.signo = signo;
    }

    public int signo() {
        return signo;
    }
}