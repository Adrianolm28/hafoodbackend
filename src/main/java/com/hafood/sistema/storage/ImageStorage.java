package com.hafood.sistema.storage;

import java.util.Optional;

public interface ImageStorage {

    String guardar(String carpeta, byte[] contenido);

    Optional<byte[]> leer(String carpeta, String archivo);

    void eliminar(String ruta);

    String copiar(String ruta);
}
