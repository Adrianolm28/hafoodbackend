package com.hafood.sistema.controller;

import com.hafood.sistema.config.TenantContext;

import com.hafood.sistema.config.TenantStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantStorageController {

    private final TenantStorageService tenantStorageService;

    @GetMapping("/{tenantId}/storage")
    public ResponseEntity<Map<String, Object>> obtenerStorage(@PathVariable String tenantId) {
        return ResponseEntity.ok(tenantStorageService.obtenerResumenStorage(tenantId));
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> obtenerStorageDelTenantActual() {
        String tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(tenantStorageService.obtenerResumenStorage(tenantId));
    }
}