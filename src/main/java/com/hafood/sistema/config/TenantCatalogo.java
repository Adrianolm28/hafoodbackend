package com.hafood.sistema.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TenantCatalogo {

    private static final String SQL = "SELECT tenant_id FROM tenants WHERE UPPER(estado) = 'ACTIVO' ORDER BY tenant_id";

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.storage.postgres-port:5432}")
    private String postgresPort;

    public List<String> listarActivos() {
        String url = "jdbc:postgresql://localhost:" + postgresPort + "/hafood_central_db";
        List<String> tenants = new ArrayList<>();

        try (Connection conexion = DriverManager.getConnection(url, username, password);
             PreparedStatement consulta = conexion.prepareStatement(SQL);
             ResultSet filas = consulta.executeQuery()) {
            while (filas.next()) {
                tenants.add(filas.getString(1));
            }
        } catch (SQLException e) {
            log.warn("No se pudo leer la lista de empresas para el envío automático: {}", e.getMessage());
        }

        return tenants;
    }
}