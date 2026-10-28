package com.ponti.api.dto;

import com.ponti.api.model.Mentor;
import java.math.BigDecimal;

// Mentor como a API devolve: os dados do perfil mais a reputação na plataforma
// (média das avaliações, quantidade de avaliações e de mentorias já realizadas).
public record MentorResposta(
        Long id,
        String nome,
        String bio,
        String areasAtuacao,
        String ecossistemas,
        Integer anosExperiencia,
        BigDecimal valorSessao,
        Integer duracaoSessaoMinutos,
        String fotoUrl,
        String videoUrl,
        String linkedinUrl,
        Double notaMedia,
        long totalAvaliacoes,
        long totalMentorias) {

    public static MentorResposta de(Mentor m, Double notaMedia, long totalAvaliacoes, long totalMentorias) {
        return new MentorResposta(m.getId(), m.getNome(), m.getBio(), m.getAreasAtuacao(), m.getEcossistemas(),
                m.getAnosExperiencia(), m.getValorSessao(), m.getDuracaoSessaoMinutos(), m.getFotoUrl(),
                m.getVideoUrl(), m.getLinkedinUrl(), notaMedia, totalAvaliacoes, totalMentorias);
    }
}
