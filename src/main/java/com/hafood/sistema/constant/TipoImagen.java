package com.hafood.sistema.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoImagen {
    FONDO_MOVIL("cartas", 1080, 1920, 80),
    FONDO_ESCRITORIO("cartas", 1920, 1080, 80),
    LOGO("cartas", 800, 800, 90),
    PRODUCTO("productos", 640, 640, 78);

    private final String carpeta;
    private final int anchoMax;
    private final int altoMax;
    private final int calidad;
}
