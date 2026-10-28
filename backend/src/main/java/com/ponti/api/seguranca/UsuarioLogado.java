package com.ponti.api.seguranca;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;

// Quem fez a requisição, lido do token JWT. Só um entre startupId e mentorId vem preenchido.
public record UsuarioLogado(Long usuarioId, TipoUsuario tipo, Long startupId, Long mentorId) {

    public static UsuarioLogado de(Usuario usuario) {
        return new UsuarioLogado(usuario.getId(), usuario.getTipo(), usuario.getStartupId(), usuario.getMentorId());
    }

    public boolean ehStartup(Long id) {
        return tipo == TipoUsuario.STARTUP && id != null && id.equals(startupId);
    }

    public boolean ehMentor(Long id) {
        return tipo == TipoUsuario.MENTOR && id != null && id.equals(mentorId);
    }
}
