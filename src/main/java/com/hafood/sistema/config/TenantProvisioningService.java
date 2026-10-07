package com.hafood.sistema.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Properties;

@Service
public class TenantProvisioningService {

    private final DynamicRoutingDataSource dynamicDataSource;
    private final TenantDataSeeder tenantDataSeeder;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.storage.postgres-port:5432}")
    private String postgresPort;

    public TenantProvisioningService(DynamicRoutingDataSource dynamicDataSource, TenantDataSeeder tenantDataSeeder) {
        this.dynamicDataSource = dynamicDataSource;
        this.tenantDataSeeder = tenantDataSeeder;
    }

    public void initTenant(String tenantId) {
        if (dynamicDataSource.hasDataSource(tenantId)) {
            return;
        }

        if (!esTenantIdValido(tenantId)) {
            throw new SecurityException("Identificador de tenant inválido");
        }

        if (!esTenantAutorizado(tenantId)) {
            throw new SecurityException("Tenant no autorizado o inactivo: " + tenantId);
        }

        String dbName = "hafood_" + tenantId;
        // IMPORTANTE: Asegúrate de usar localhost o 127.0.0.1 según te funcione mejor
        String masterUrl = "jdbc:postgresql://localhost:" + postgresPort + "/hafood_central_db";

        try {
            boolean isNewDatabase = false;

            try (Connection masterConn = DriverManager.getConnection(masterUrl, username, password)) {
                String checkDbSql = "SELECT 1 FROM pg_database WHERE datname = ?";
                boolean dbExists = false;
                try (PreparedStatement ps = masterConn.prepareStatement(checkDbSql)) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            dbExists = true;
                        }
                    }
                }

                if (!dbExists) {
                    try (Statement statement = masterConn.createStatement()) {
                        statement.executeUpdate("CREATE DATABASE " + dbName);
                        isNewDatabase = true;
                        System.out.println("Base de datos PostgreSQL NUEVA creada: " + dbName);
                    }
                }
            }

            String tenantUrl = "jdbc:postgresql://localhost:" + postgresPort + "/" + dbName;
            HikariDataSource tenantDataSource = new HikariDataSource();
            tenantDataSource.setDriverClassName("org.postgresql.Driver");
            tenantDataSource.setJdbcUrl(tenantUrl);
            tenantDataSource.setUsername(username);
            tenantDataSource.setPassword(password);
            tenantDataSource.setMaximumPoolSize(10);

            generarEsquemaAutomaticamente(tenantDataSource);

            dynamicDataSource.addDataSource(tenantId, tenantDataSource);
            System.out.println("Conexión enrutada para: " + tenantId);

            if (isNewDatabase) {
                tenantDataSeeder.sembrarAdminMaestro();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando tenant: " + e.getMessage(), e);

        }
    }

    private void generarEsquemaAutomaticamente(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
        emfb.setDataSource(dataSource);
        emfb.setPackagesToScan("com.hafood.sistema.domain");

        emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "true");
        emfb.setJpaProperties(properties);

        emfb.afterPropertiesSet();
        emfb.destroy();
    }

    private boolean esTenantIdValido(String tenantId) {
        return tenantId != null && tenantId.matches("^[a-z0-9-]{3,50}$");
    }

    private boolean esTenantAutorizado(String tenantId) {
        String masterUrl = "jdbc:postgresql://localhost:" + postgresPort + "/hafood_central_db";
        String sql = "SELECT estado FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "ACTIVO".equalsIgnoreCase(rs.getString("estado"));
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}