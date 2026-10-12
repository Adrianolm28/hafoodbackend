package com.hafood.sistema.service;

import com.hafood.sistema.config.TenantCatalogo;
import com.hafood.sistema.config.TenantContext;
import com.hafood.sistema.config.TenantProvisioningService;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.EnvioMasivoDTO;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hafood.sunat", name = "automatico-activo", havingValue = "true", matchIfMissing = true)
public class SunatEnvioProgramado {

    private static final String ACTOR = "SUPERADMIN";

    private final TenantCatalogo catalogo;
    private final TenantProvisioningService provisioning;
    private final UsuarioRepository usuarioRepository;
    private final SunatEnvioService envioService;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @Scheduled(initialDelayString = "${hafood.sunat.automatico-inicio-ms:60000}",
            fixedDelayString = "${hafood.sunat.automatico-intervalo-ms:300000}")
    public void ejecutar() {
        if (!ejecutando.compareAndSet(false, true)) {
            return;
        }

        try {
            for (String tenant : catalogo.listarActivos()) {
                procesar(tenant);
            }
        } finally {
            ejecutando.set(false);
        }
    }

    private void procesar(String tenant) {
        try {
            TenantContext.setCurrentTenant(tenant);
            provisioning.initTenant(tenant);

            Usuario actor = usuarioRepository.findByUsername(ACTOR).orElse(null);

            if (actor == null) {
                log.warn("Envío automático omitido en {}: no existe el usuario {}", tenant, ACTOR);
                return;
            }

            EnvioMasivoDTO r = envioService.enviarAutomatico(actor);

            if (r.aceptados() + r.rechazados() + r.conError() + r.omitidos() > 0) {
                log.info("Envío automático {}: aceptados={} rechazados={} conError={} omitidos={} quedanMas={}",
                        tenant, r.aceptados(), r.rechazados(), r.conError(), r.omitidos(), r.quedanMas());
            }
        } catch (RuntimeException e) {
            log.warn("Envío automático falló en {}: {}", tenant, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}