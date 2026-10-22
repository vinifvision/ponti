package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;

// Dados públicos do usuário (cadastro, login e /auth/me) e o id do perfil (startupId ou mentorId).
// Não inclui a senha nem o hash.
public record UsuarioResumo(
        Long id,
        String email,
        TipoUsuario tipo,
        Long startupId,
        Long mentorId) {

    public static UsuarioResumo de(Usuario usuario) {
        return new UsuarioResumo(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTipo(),
                usuario.getStartupId(),
                usuario.getMentorId());
    }
}
