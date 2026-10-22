package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;

// Dados públicos do usuário (cadastro, login e /auth/me): o id do perfil (startupId ou mentorId)
// e o nome da startup ou do mentor, para o front mostrar quem está logado.
// Não inclui a senha nem o hash.
public record UsuarioResumo(
        Long id,
        String email,
        TipoUsuario tipo,
        Long startupId,
        Long mentorId,
        String nome) {

    public static UsuarioResumo de(Usuario usuario, String nome) {
        return new UsuarioResumo(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTipo(),
                usuario.getStartupId(),
                usuario.getMentorId(),
                nome);
    }
}
