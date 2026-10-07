package com.hafood.sistema.config;

import com.hafood.sistema.constant.Role;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        sembrarAdminMaestro();
    }

    @Transactional
    public void sembrarAdminMaestro() {
        if (usuarioRepository.findByUsername("SUPERADMIN").isEmpty()) {
            Usuario superAdmin = Usuario.builder()
                    .username("SUPERADMIN")
                    .email("admin@hacont.com")
                    .password(passwordEncoder.encode("sis0te1ma2con3tabl4--h4c0nt"))
                    .role(Role.ADMIN)
                    .superAdmin(true)
                    .intentosFallidos(0)
                    .bloqueadoHasta(null)
                    .build();

            usuarioRepository.save(superAdmin);
            log.info(">>> [SEEDER] Llave Maestra (Usuario SUPERADMIN) inicializada correctamente.");
        } else {
            log.info(">>> [SEEDER] El usuario SUPERADMIN ya existe en el sistema.");
        }
    }
}