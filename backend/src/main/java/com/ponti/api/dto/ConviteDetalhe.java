package com.ponti.api.dto;

import com.ponti.api.model.StatusConvite;
import com.ponti.api.model.TipoUsuario;
import java.math.BigDecimal;

// Convite já com os dados da startup e do mentor, e de quem foi a iniciativa (enviadoPor).
// Serve para o front montar as listas de recebidos e enviados dos dois lados
// sem precisar de uma chamada extra por convite.
public record ConviteDetalhe(
        Long id,
        StatusConvite status,
        TipoUsuario enviadoPor,
        StartupResumo startup,
        MentorResumo mentor) {

    // O site da startup só aparece depois que o mentor aceita (privacidade antes do match).
    public record StartupResumo(
            Long id,
            String nome,
            String descricao,
            String segmento,
            String cidade,
            String estado,
            Integer estagioMaturidade,
            String videoPitchUrl,
            String fotoUrl,
            String site) {
    }

    public record MentorResumo(
            Long id,
            String nome,
            String areasAtuacao,
            String ecossistemas,
            BigDecimal valorSessao,
            Integer duracaoSessaoMinutos,
            String fotoUrl,
            String linkedinUrl) {
    }
}
