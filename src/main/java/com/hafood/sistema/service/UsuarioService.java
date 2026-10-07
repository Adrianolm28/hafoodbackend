package com.hafood.sistema.service;

import com.hafood.sistema.constant.Role;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.UsuarioDTO;
import com.hafood.sistema.exception.NotFoundException;
import com.hafood.sistema.exception.ValidationException;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.SedeRepository;
import com.hafood.sistema.repository.SeccionRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import com.hafood.sistema.service.impl.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository repo;
    private final SedeRepository sedeRepo;
    private final SeccionRepository seccionRepo;
    private final PasswordEncoder passwordEncoder;

    private static final java.util.regex.Pattern PATRON_PASSWORD_SEGURA =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    @Override
    public List<UsuarioDTO> mostrarUsuarios() {
        return repo.findAll().stream().map(Mapper::toDTO).toList();
    }

    @Override
    @Transactional
    public UsuarioDTO agregarUsuario(UsuarioDTO usuarioDTO, Usuario actor) {

        validarPermisoParaAsignarRol(usuarioDTO.getRole(), actor);

        // Validación de Sede
        Sede sedeBase = null;
        if (usuarioDTO.getSedeId() != null) {
            sedeBase = sedeRepo.findById(usuarioDTO.getSedeId())
                    .orElseThrow(() -> new NotFoundException("La sede base especificada no existe"));
        } else if (usuarioDTO.getRole() == Role.CAJERO || usuarioDTO.getRole() == Role.MOZO || usuarioDTO.getRole() == Role.BARTENDER) {
            throw new ValidationException("Los Cajeros, Mozos y Bartenders deben tener una Sede asignada obligatoriamente.");
        }

        // Validación de Sección
        Seccion seccionBase = null;
        if (usuarioDTO.getSeccionId() != null) {
            seccionBase = seccionRepo.findById(usuarioDTO.getSeccionId())
                    .orElseThrow(() -> new NotFoundException("La sección especificada no existe"));
        }

        validarFortalezaPassword(usuarioDTO.getPassword());

        Usuario u = Usuario.builder()
                .username(usuarioDTO.getUsername())
                .email(usuarioDTO.getEmail())
                .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                .role(usuarioDTO.getRole())
                .sede(sedeBase)
                .seccion(seccionBase)
                .superAdmin(false) // El flag de superAdmin SOLO se asigna mediante el Seeder inicial
                .build();

        return Mapper.toDTO(repo.save(u));
    }

    private void validarPermisoParaAsignarRol(Role rolSolicitado, Usuario actor) {
        if (rolSolicitado == Role.ADMIN) {
            if (actor == null || !actor.isSuperAdmin()) {
                throw new ValidationException("Solo un Administrador Supremo puede otorgar el rol ADMIN");
            }
            long totalAdmins = repo.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();
            if (totalAdmins >= 5) {
                throw new ValidationException("Ya existe el máximo de administradores permitidos (5) para este sistema");
            }
        }
    }

    private void validarFortalezaPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Debe asignar una contraseña para el usuario");
        }
        if (!PATRON_PASSWORD_SEGURA.matcher(password).matches()) {
            throw new ValidationException(
                    "La contraseña debe tener mínimo 8 caracteres, con al menos una mayúscula, " +
                            "una minúscula, un número y un carácter especial"
            );
        }
    }

    @Override
    @Transactional
    public UsuarioDTO actualizamosUsuario(Long id, UsuarioDTO usuarioDTO, Usuario actor) {
        Usuario u = repo.findById(id).orElseThrow(
                () -> new NotFoundException("Usuario no encontrado para actualizar")
        );

        if (u.isSuperAdmin()) {
            throw new ValidationException("El Administrador supremo del sistema no puede ser modificado");
        }

        if (u.getRole() == Role.ADMIN && (actor == null || !actor.isSuperAdmin())) {
            throw new ValidationException("Solo el Administrador supremo puede modificar a otro Administrador");
        }

        if (usuarioDTO.getRole() != null && usuarioDTO.getRole() != u.getRole()) {
            validarPermisoParaAsignarRol(usuarioDTO.getRole(), actor);
            u.setRole(usuarioDTO.getRole());
        }

        u.setUsername(usuarioDTO.getUsername());
        u.setEmail(usuarioDTO.getEmail());

        if (usuarioDTO.getSedeId() != null) {
            Sede nuevaSede = sedeRepo.findById(usuarioDTO.getSedeId())
                    .orElseThrow(() -> new NotFoundException("La sede especificada no existe"));
            u.setSede(nuevaSede);
        } else {
            u.setSede(null);
        }

        if (usuarioDTO.getSeccionId() != null) {
            Seccion nuevaSeccion = seccionRepo.findById(usuarioDTO.getSeccionId())
                    .orElseThrow(() -> new NotFoundException("La sección especificada no existe"));
            u.setSeccion(nuevaSeccion);
        } else {
            u.setSeccion(null);
        }

        return Mapper.toDTO(repo.save(u));
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id, Usuario actor) {
        Usuario u = repo.findById(id).orElseThrow(
                () -> new NotFoundException("Usuario no encontrado para eliminar")
        );

        if (u.isSuperAdmin()) {
            throw new ValidationException("Operación denegada: El Administrador supremo del sistema no puede ser eliminado.");
        }

        if (u.getRole() == Role.ADMIN && (actor == null || !actor.isSuperAdmin())) {
            throw new ValidationException("Solo el Administrador supremo puede eliminar a otro Administrador");
        }

        repo.deleteById(id);
    }

    @Override
    public UsuarioDTO buscarPorNombre(String username) {
        return repo.findByUsername(username)
                .map(Mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado en el filtro de búsqueda: " + username));
    }

    @Override
    public UsuarioDTO buscarPorEmail(String email) {
        return repo.findByEmail(email)
                .map(Mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Email no encontrado en el filtro de búsqueda: " + email));
    }
}