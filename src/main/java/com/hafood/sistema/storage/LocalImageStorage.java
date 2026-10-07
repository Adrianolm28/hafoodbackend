package com.hafood.sistema.storage;

import com.hafood.sistema.config.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LocalImageStorage implements ImageStorage {

    private static final Pattern TENANT = Pattern.compile("^[a-z0-9-]{3,50}$");
    private static final Pattern RUTA = Pattern.compile(
            "^(cartas|productos)/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp$");

    private final Path raiz;

    public LocalImageStorage(@Value("${app.storage.images-path:./uploads}") String ruta) {
        this.raiz = Path.of(ruta).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, byte[] contenido) {
        String ruta = carpeta + "/" + UUID.randomUUID() + ".webp";
        Path destino = resolver(ruta);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido, StandardOpenOption.CREATE_NEW);
            return ruta;
        } catch (IOException e) {
            log.error("No se pudo guardar la imagen {}", ruta, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }

    @Override
    public Optional<byte[]> leer(String carpeta, String archivo) {
        String ruta = carpeta + "/" + archivo;
        if (!RUTA.matcher(ruta).matches()) {
            return Optional.empty();
        }
        Path origen = resolver(ruta);
        try {
            return Files.isRegularFile(origen) ? Optional.of(Files.readAllBytes(origen)) : Optional.empty();
        } catch (IOException e) {
            log.error("No se pudo leer la imagen {}", ruta, e);
            return Optional.empty();
        }
    }

    @Override
    public void eliminar(String ruta) {
        if (ruta == null || !RUTA.matcher(ruta).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException e) {
            log.warn("No se pudo eliminar la imagen {}", ruta, e);
        }
    }

    @Override
    public String copiar(String ruta) {
        if (ruta == null || !RUTA.matcher(ruta).matches()) {
            return null;
        }
        try {
            byte[] contenido = Files.readAllBytes(resolver(ruta));
            return guardar(ruta.substring(0, ruta.indexOf('/')), contenido);
        } catch (IOException e) {
            log.warn("No se pudo copiar la imagen {}", ruta, e);
            return null;
        }
    }

    private Path resolver(String ruta) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || "public".equals(tenant) || !TENANT.matcher(tenant).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo determinar el restaurante");
        }
        Path base = raiz.resolve(tenant).normalize();
        Path resuelta = base.resolve(ruta).normalize();
        if (!resuelta.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de imagen no válida");
        }
        return resuelta;
    }
}
