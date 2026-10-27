package com.ponti.api.dto;

import com.ponti.api.model.StatusConvite;
import java.math.BigDecimal;

// Convite já com os dados de quem enviou (startup) e de quem recebeu (mentor).
// Serve para o front montar a inbox do mentor e a lista de enviados da startup
// sem precisar de uma chamada extra por convite.
public record ConviteDetalhe(
        Long id,
        StatusConvite status,
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
            String linkedinUrl,
            // O WhatsApp do mentor só aparece depois que o convite é aceito
            String whatsapp) {
    }
}
