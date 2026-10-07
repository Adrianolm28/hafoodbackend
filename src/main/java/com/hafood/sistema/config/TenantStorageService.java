package com.hafood.sistema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class TenantStorageService {

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.storage.postgres-port:5432}")
    private String postgresPort;

    public Map<String, Object> obtenerResumenStorage(String tenantId) {
        if (tenantId == null || !tenantId.matches("^[a-z0-9-]{3,50}$")) {
            throw new SecurityException("Identificador de tenant inválido: " + tenantId);
        }

        long bytesBD = obtenerTamanoBD(tenantId);
        long bytesUploads = obtenerTamanoUploads(tenantId);
        long bytesTotal = bytesBD + bytesUploads;

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("tenantId", tenantId);
        resumen.put("baseDatosMB", redondear(bytesBD));
        resumen.put("uploadsMB", redondear(bytesUploads));
        resumen.put("totalMB", redondear(bytesTotal));
        return resumen;
    }

    private double redondear(long bytes) {
        return Math.round((bytes / (1024.0 * 1024.0)) * 100.0) / 100.0;
    }

    private long obtenerTamanoBD(String tenantId) {
        String dbName = "hafood_" + tenantId;
        String url = "jdbc:postgresql://localhost:" + postgresPort + "/" + dbName;

        // Función nativa de PostgreSQL para obtener el tamaño exacto en bytes
        String sql = "SELECT pg_database_size(?)";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG STORAGE] Error consultando almacenamiento en Postgres para " + dbName + ": " + e.getMessage());
        }
        return 0L;
    }

    private long obtenerTamanoUploads(String tenantId) {
        Path carpeta = Paths.get("uploads", tenantId);
        if (!Files.exists(carpeta)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(carpeta)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            System.err.println("Error midiendo carpeta uploads de " + tenantId + ": " + e.getMessage());
            return 0L;
        }
    }
}