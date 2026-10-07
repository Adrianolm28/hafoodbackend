package com.hafood.sistema.service.impl;

import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.UsuarioDTO;
import java.util.List;

public interface IUsuarioService {
    List<UsuarioDTO> mostrarUsuarios();
    UsuarioDTO agregarUsuario(UsuarioDTO usuarioDTO, Usuario actor);
    UsuarioDTO actualizamosUsuario(Long id, UsuarioDTO usuarioDTO, Usuario actor);
    void eliminarUsuario(Long id, Usuario actor);
    UsuarioDTO buscarPorNombre(String username);
    UsuarioDTO buscarPorEmail(String email);
}