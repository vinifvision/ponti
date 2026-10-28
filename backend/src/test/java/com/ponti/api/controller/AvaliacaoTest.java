package com.ponti.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Avaliações: só a startup da mentoria avalia, só depois que ela aconteceu e só uma vez.
// A nota média e a quantidade de mentorias aparecem no perfil do mentor.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvaliacaoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AgendamentoRepository agendamentoRepository;

    Long mentorId;
    Long startupId;

    @BeforeEach
    void preparar() {
        Mentor m = new Mentor();
        m.setNome("Mentor Avaliado");
        mentorId = mentorRepository.save(m).getId();
        Startup s = new Startup();
        s.setNome("Startup Avaliadora");
        startupId = startupRepository.save(s).getId();
    }

    Long sessao(LocalDateTime inicio, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setStartupId(startupId);
        a.setMentorId(mentorId);
        a.setInicio(inicio);
        a.setFim(inicio.plusHours(1));
        a.setValor(BigDecimal.ZERO);
        a.setStatus(status);
        a.setStatusPagamento(StatusPagamento.GRATUITA);
        return agendamentoRepository.save(a).getId();
    }

    ResultActions avaliar(Long id, Long comoStartup, int nota, String comentario) throws Exception {
        return mockMvc.perform(post("/agendamentos/" + id + "/avaliacao").with(Login.comoStartup(comoStartup))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nota", nota, "comentario", comentario))));
    }

    LocalDateTime agora() {
        return LocalDateTime.now(RelogioConfig.FUSO);
    }

    @Test
    void avaliaMentoriaRealizadaEAtualizaAReputacao() throws Exception {
        Long id1 = sessao(agora().minusDays(3), StatusAgendamento.CONFIRMADO);
        Long id2 = sessao(agora().minusDays(10), StatusAgendamento.CONFIRMADO);

        avaliar(id1, startupId, 5, "Excelente")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.realizada").value(true))
                .andExpect(jsonPath("$.avaliacao.nota").value(5));
        avaliar(id2, startupId, 4, "Muito boa").andExpect(status().isCreated());

        mockMvc.perform(get("/mentores/" + mentorId).with(Login.comoStartup(startupId)))
                .andExpect(jsonPath("$.notaMedia").value(4.5))
                .andExpect(jsonPath("$.totalAvaliacoes").value(2))
                .andExpect(jsonPath("$.totalMentorias").value(2));
        mockMvc.perform(get("/mentores/" + mentorId + "/avaliacoes").with(Login.comoStartup(startupId)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startupNome").value("Startup Avaliadora"));
    }

    @Test
    void naoAvaliaAntesDeAcontecerNemDuasVezes() throws Exception {
        Long futura = sessao(agora().plusDays(3), StatusAgendamento.CONFIRMADO);
        Long cancelada = sessao(agora().minusDays(3), StatusAgendamento.CANCELADO);
        Long realizada = sessao(agora().minusDays(3).minusHours(5), StatusAgendamento.CONFIRMADO);

        avaliar(futura, startupId, 5, "").andExpect(status().isConflict());
        avaliar(cancelada, startupId, 5, "").andExpect(status().isConflict());
        avaliar(realizada, startupId, 5, "").andExpect(status().isCreated());
        avaliar(realizada, startupId, 4, "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Essa mentoria já foi avaliada"));
    }

    @Test
    void soAStartupDaMentoriaAvaliaComNotaDe1a5() throws Exception {
        Long id = sessao(agora().minusDays(2), StatusAgendamento.CONFIRMADO);

        avaliar(id, startupId + 999, 5, "").andExpect(status().isForbidden());
        avaliar(id, startupId, 6, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nota"));
    }

    @Test
    void mentorSemAvaliacoesTemNotaNula() throws Exception {
        mockMvc.perform(get("/mentores/" + mentorId).with(Login.comoStartup(startupId)))
                .andExpect(jsonPath("$.notaMedia").doesNotExist())
                .andExpect(jsonPath("$.totalAvaliacoes").value(0));
    }
}
