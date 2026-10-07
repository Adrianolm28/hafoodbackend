package com.hafood.sistema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenantMigrationService {

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.storage.postgres-port:5432}")
    private String postgresPort;

    private static final List<String> MIGRACIONES = List.of(
            "ALTER TABLE insumos ADD COLUMN IF NOT EXISTS unidad_base VARCHAR(10) NOT NULL DEFAULT 'UNIDAD'",
            "ALTER TABLE insumos ADD COLUMN IF NOT EXISTS presentacion_nombre VARCHAR(30)",
            "ALTER TABLE insumos ADD COLUMN IF NOT EXISTS presentacion_cantidad NUMERIC(12,3)",
            "ALTER TABLE insumos ADD COLUMN IF NOT EXISTS control_estricto BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE insumos ALTER COLUMN costo_unitario TYPE NUMERIC(14,6)"
    );

    public List<String> migrarTenant(String tenantId) {
        if (tenantId == null || !tenantId.matches("^[a-z0-9-]{3,50}$")) {
            throw new SecurityException("Identificador de tenant inválido: " + tenantId);
        }

        String dbName = "hafood_" + tenantId;
        String url = "jdbc:postgresql://localhost:" + postgresPort + "/" + dbName;

        List<String> resultados = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            for (String sql : MIGRACIONES) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(sql);
                    resultados.add("OK -> " + sql.substring(0, Math.min(70, sql.length())) + "...");
                } catch (SQLException e) {
                    resultados.add("ERROR (" + e.getMessage() + ") -> " + sql.substring(0, Math.min(70, sql.length())) + "...");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar al tenant " + tenantId + ": " + e.getMessage());
        }
        return resultados;
    }
}