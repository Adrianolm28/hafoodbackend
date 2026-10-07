package com.hafood.sistema.mapper;

public final class ImagenUrl {

    private static final String BASE = "/api/v1/publico/imagenes/";

    private ImagenUrl() {
    }

    public static String de(String ruta) {
        return ruta == null || ruta.isBlank() ? null : BASE + ruta;
    }
}
