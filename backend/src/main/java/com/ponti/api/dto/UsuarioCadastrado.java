package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;

// Resposta do cadastro: quem foi criado e o id do perfil (startupId ou mentorId).
// Não inclui a senha nem o hash.
public record UsuarioCadastrado(
        Long id,
        String email,
        TipoUsuario tipo,
        Long startupId,
        Long mentorId) {

    public static UsuarioCadastrado de(Usuario usuario) {
        return new UsuarioCadastrado(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTipo(),
                usuario.getStartupId(),
                usuario.getMentorId());
    }
}
