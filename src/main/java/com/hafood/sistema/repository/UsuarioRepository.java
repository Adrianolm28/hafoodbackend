package com.hafood.sistema.repository;

import com.hafood.sistema.domain.user.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsBySedeId(Long sedeId);
    boolean existsBySeccionId(Long seccionId);
    Optional<Usuario> findByEmail(String email);
}