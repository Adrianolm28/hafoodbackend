package com.hafood.sistema.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TenantProvisioningService tenantProvisioningService;
    private static final String DOMINIO_RAIZ = "hafood.com";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String serverName = request.getServerName();
        String tenantId = resolverTenantId(serverName);
        logger.info("Host recibido: " + serverName + " -> tenant: " + tenantId);

        TenantContext.setCurrentTenant(tenantId);

        try {
            if (!tenantId.equals("public")) {
                tenantProvisioningService.initTenant(tenantId);
            }
        } catch (SecurityException e) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Restaurante no registrado o inactivo");
            return;
        } catch (RuntimeException e) {
            logger.error("Error inicializando el tenant " + tenantId, e);
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error conectando con la base de datos del restaurante");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolverTenantId(String serverName) {
        if (serverName.equals("localhost") || serverName.equals("127.0.0.1") || serverName.equals(DOMINIO_RAIZ) || serverName.equals("www." + DOMINIO_RAIZ)) {
            return "public";
        }
        if (serverName.endsWith("." + DOMINIO_RAIZ)) {
            return serverName.substring(0, serverName.length() - ("." + DOMINIO_RAIZ).length());
        }
        return serverName.split("\\.")[0];
    }
}