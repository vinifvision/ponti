package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import java.time.Instant;

// Linha da lista de usuários do admin. Só o necessário para administrar a conta:
// nada de senha, WhatsApp ou dados do perfil (LGPD: minimização).
public record UsuarioAdmin(
        Long id,
        String email,
        TipoUsuario tipo,
        String nome,
        Long perfilId,
        boolean bloqueado,
        Instant bloqueadoEm,
        String motivoBloqueio,
        Instant criadoEm,
        Instant ultimoAcessoEm) {

    public static UsuarioAdmin de(Usuario u, String nome) {
        Long perfilId = u.getStartupId() != null ? u.getStartupId() : u.getMentorId();
        return new UsuarioAdmin(u.getId(), u.getEmail(), u.getTipo(), nome, perfilId, u.estaBloqueado(),
                u.getBloqueadoEm(), u.getMotivoBloqueio(), u.getCriadoEm(), u.getUltimoAcessoEm());
    }
}
