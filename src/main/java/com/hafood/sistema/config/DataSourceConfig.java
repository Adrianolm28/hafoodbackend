package com.hafood.sistema.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.storage.postgres-port:5432}")
    private String postgresPort;

    @Bean
    @Primary
    public DynamicRoutingDataSource dataSource() {
        DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();

        HikariDataSource masterDataSource = new HikariDataSource();
        masterDataSource.setDriverClassName("org.postgresql.Driver");
        masterDataSource.setJdbcUrl("jdbc:postgresql://localhost:" + postgresPort + "/hafood_central_db");
        masterDataSource.setUsername(username);
        masterDataSource.setPassword(password);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("public", masterDataSource);

        dynamicRoutingDataSource.setDefaultTargetDataSource(masterDataSource);
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);

        return dynamicRoutingDataSource;
    }
}