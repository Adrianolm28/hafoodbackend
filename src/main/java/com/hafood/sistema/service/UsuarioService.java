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

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private static final Set<Role> ROLES_CON_SEDE =
            EnumSet.of(Role.ENCARGADO_SEDE, Role.CAJERO, Role.MOZO, Role.BARTENDER);
    private static final int MAX_ADMINS = 5;
    private static final int MAX_BYTES_PASSWORD = 72;
    private static final Pattern PATRON_PASSWORD_SEGURA =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    private final UsuarioRepository repo;
    private final SedeRepository sedeRepo;
    private final SeccionRepository seccionRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioDTO> mostrarUsuarios() {
        return repo.findAll().stream().map(this::aDto).toList();
    }

    @Override
    @Transactional
    public UsuarioDTO agregarUsuario(UsuarioDTO usuarioDTO, Usuario actor) {
        Role rol = usuarioDTO.getRole();

        if (rol == null) {
            throw new ValidationException("Elige el rol del usuario");
        }

        validarPermisoParaAsignarRol(rol, actor);
        Sede sede = resolverSede(usuarioDTO.getSedeId(), rol);
        Seccion seccion = resolverSeccion(usuarioDTO.getSeccionId());
        validarFortalezaPassword(usuarioDTO.getPassword());

        Usuario u = Usuario.builder()
                .username(usuarioDTO.getUsername())
                .email(usuarioDTO.getEmail())
                .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                .role(rol)
                .sede(sede)
                .seccion(seccion)
                .superAdmin(false)
                .activo(true)
                .build();

        return aDto(repo.save(u));
    }

    @Override
    @Transactional
    public UsuarioDTO actualizamosUsuario(Long id, UsuarioDTO usuarioDTO, Usuario actor) {
        Usuario u = buscar(id, "Usuario no encontrado para actualizar");
        validarGestionable(u, actor);

        Role rolFinal = usuarioDTO.getRole() != null ? usuarioDTO.getRole() : u.getRole();

        if (rolFinal != u.getRole()) {
            validarPermisoParaAsignarRol(rolFinal, actor);
        }

        Sede sede = resolverSede(usuarioDTO.getSedeId(), rolFinal);
        Seccion seccion = resolverSeccion(usuarioDTO.getSeccionId());

        u.setRole(rolFinal);
        u.setUsername(usuarioDTO.getUsername());
        u.setEmail(usuarioDTO.getEmail());
        u.setSede(sede);
        u.setSeccion(seccion);

        return aDto(repo.save(u));
    }

    @Transactional
    public void restablecerPassword(Long id, String password, Usuario actor) {
        Usuario u = buscar(id, "Usuario no encontrado");
        boolean propio = actor != null && u.getId().equals(actor.getId());

        if (esSupremo(u)) {
            if (!propio) {
                throw new ValidationException("La contraseña del Administrador supremo solo la cambia él mismo");
            }
        } else {
            validarGestionable(u, actor);
        }

        validarFortalezaPassword(password);
        u.setPassword(passwordEncoder.encode(password));
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        repo.save(u);
    }

    @Transactional
    public UsuarioDTO cambiarEstado(Long id, boolean activo, Usuario actor) {
        Usuario u = buscar(id, "Usuario no encontrado");
        validarGestionable(u, actor);

        if (!activo && actor != null && u.getId().equals(actor.getId())) {
            throw new ValidationException("No puedes desactivar tu propio usuario");
        }

        u.setActivo(activo);
        return aDto(repo.save(u));
    }



    @Override
    public UsuarioDTO buscarPorNombre(String username) {
        return repo.findByUsername(username)
                .map(this::aDto)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado en el filtro de búsqueda: " + username));
    }

    @Override
    public UsuarioDTO buscarPorEmail(String email) {
        return repo.findByEmail(email)
                .map(this::aDto)
                .orElseThrow(() -> new NotFoundException("Email no encontrado en el filtro de búsqueda: " + email));
    }

    private UsuarioDTO aDto(Usuario u) {
        UsuarioDTO dto = Mapper.toDTO(u);
        dto.setActivo(u.isActivo());
        return dto;
    }

    private Usuario buscar(Long id, String mensaje) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException(mensaje));
    }

    private Sede resolverSede(Long sedeId, Role rol) {
        if (sedeId == null) {
            if (ROLES_CON_SEDE.contains(rol)) {
                throw new ValidationException("Los Encargados, Cajeros, Mozos y Bartenders deben tener una Sede asignada obligatoriamente.");
            }
            return null;
        }

        return sedeRepo.findById(sedeId)
                .orElseThrow(() -> new NotFoundException("La sede especificada no existe"));
    }

    private Seccion resolverSeccion(Long seccionId) {
        if (seccionId == null) {
            return null;
        }

        return seccionRepo.findById(seccionId)
                .orElseThrow(() -> new NotFoundException("La sección especificada no existe"));
    }

    private boolean esSupremo(Usuario u) {
        return u.isSuperAdmin() || u.getRole() == Role.SUPERADMIN;
    }

    private boolean esSuperAdmin(Usuario actor) {
        return actor != null && actor.isSuperAdmin();
    }

    private void validarGestionable(Usuario objetivo, Usuario actor) {
        if (esSupremo(objetivo)) {
            throw new ValidationException("El Administrador supremo del sistema no puede ser modificado");
        }
        if (objetivo.getRole() == Role.ADMIN && !esSuperAdmin(actor)) {
            throw new ValidationException("Solo el Administrador supremo puede modificar a otro Administrador");
        }
    }

    private void validarPermisoParaAsignarRol(Role rolSolicitado, Usuario actor) {
        if (rolSolicitado == Role.SUPERADMIN) {
            throw new ValidationException("El rol de Administrador supremo no se puede asignar");
        }

        if (rolSolicitado == Role.ADMIN) {
            if (!esSuperAdmin(actor)) {
                throw new ValidationException("Solo un Administrador Supremo puede otorgar el rol ADMIN");
            }

            long totalAdmins = repo.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();

            if (totalAdmins >= MAX_ADMINS) {
                throw new ValidationException("Ya existe el máximo de administradores permitidos (5) para este sistema");
            }
        }
    }

    private void validarFortalezaPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Debe asignar una contraseña para el usuario");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            throw new ValidationException("La contraseña es demasiado larga");
        }
        if (!PATRON_PASSWORD_SEGURA.matcher(password).matches()) {
            throw new ValidationException(
                    "La contraseña debe tener mínimo 8 caracteres, con al menos una mayúscula, "
                            + "una minúscula, un número y un carácter especial"
            );
        }
    }
}