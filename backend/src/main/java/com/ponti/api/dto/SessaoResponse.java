package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;

// O que o front recebe ao entrar ou se cadastrar: quem é a pessoa e qual é o perfil dela.
// Só um entre startupId e mentorId vem preenchido, conforme o tipo.
public record SessaoResponse(
        Long usuarioId,
        String usuario,
        TipoUsuario tipo,
        Long startupId,
        Long mentorId,
        String nome) {
}
