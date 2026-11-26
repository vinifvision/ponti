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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

// Acompanhamentos, parte 1: proposta do mentor, resposta da startup, lista e detalhe,
// com as regras de quem pode ver e mexer.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcompanhamentoPropostaTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AgendamentoRepository agendamentoRepository;

    Long mentorId;
    Long startupId;
    Long outraStartupId;
    Long outroMentorId;

    @BeforeEach
    void preparar() {
        mentorId = novoMentor("Mentora do Teste");
        outroMentorId = novoMentor("Outro Mentor");
        startupId = novaStartup("Startup do Teste");
        outraStartupId = novaStartup("Outra Startup");
        mentoriaRealizada(mentorId, startupId);
    }

    // ---------------------------------------------------------------- proposta

    @Test
    void mentorComMentoriaRealizadaPropoe() throws Exception {
        propor(Login.comoMentor(mentorId), startupId, "Validar o MVP.", List.of("Fechar 3 pilotos", "Definir preço", " "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSTO"))
                .andExpect(jsonPath("$.objetivo").value("Validar o MVP."))
                .andExpect(jsonPath("$.metasSugeridas").value(2))
                .andExpect(jsonPath("$.metasTotal").value(0))
                .andExpect(jsonPath("$.parceiro.id").value(startupId))
                .andExpect(jsonPath("$.parceiro.nome").value("Startup do Teste"))
                .andExpect(jsonPath("$.parceiro.subtitulo").value("Nível 4, Pet"));
    }

    @Test
    void naoPropoeSemMentoriaRealizada() throws Exception {
        propor(Login.comoMentor(mentorId), outraStartupId, "Objetivo", List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Você só pode propor um acompanhamento a uma startup com quem já teve uma mentoria realizada."));
    }

    @Test
    void mentoriaConfirmadaNoFuturoAindaNaoConta() throws Exception {
        Agendamento a = agendamento(mentorId, outraStartupId, LocalDateTime.now(RelogioConfig.FUSO).plusDays(2));
        a.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(a);
        propor(Login.comoMentor(mentorId), outraStartupId, "Objetivo", List.of()).andExpect(status().isConflict());
    }

    @Test
    void naoPropoeDuasVezesParaAMesmaStartup() throws Exception {
        propor(Login.comoMentor(mentorId), startupId, "Primeira", List.of()).andExpect(status().isCreated());
        propor(Login.comoMentor(mentorId), startupId, "Segunda", List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe uma proposta ou um acompanhamento em andamento com esta startup."));
    }

    @Test
    void startupNaoPodePropor() throws Exception {
        propor(Login.comoStartup(startupId), startupId, "Objetivo", List.of()).andExpect(status().isForbidden());
    }

    @Test
    void objetivoVazioOuMetasDemaisSaoRecusados() throws Exception {
        propor(Login.comoMentor(mentorId), startupId, "  ", List.of()).andExpect(status().isBadRequest());
        propor(Login.comoMentor(mentorId), startupId, "Objetivo", List.of("1", "2", "3", "4", "5", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startupInexistente() throws Exception {
        propor(Login.comoMentor(mentorId), 999_999L, "Objetivo", List.of()).andExpect(status().isNotFound());
    }

    @Test
    void semLoginNaoEntra() throws Exception {
        mockMvc.perform(get("/acompanhamentos")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- resposta da startup

    @Test
    void startupAceitaEOAcompanhamentoFicaAtivo() throws Exception {
        long id = proporOk();
        mockMvc.perform(post("/acompanhamentos/" + id + "/aceitar").with(Login.comoStartup(startupId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/acompanhamentos/" + id).with(Login.comoStartup(startupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.dataInicio").value(LocalDate.now(RelogioConfig.FUSO).toString()))
                .andExpect(jsonPath("$.metasTotal").value(2))
                .andExpect(jsonPath("$.metasCumpridas").value(0))
                .andExpect(jsonPath("$.metasSugeridas").doesNotExist())
                .andExpect(jsonPath("$.parceiro.nome").value("Mentora do Teste"));
    }

    @Test
    void naoAceitaDuasVezes() throws Exception {
        long id = proporOk();
        mockMvc.perform(post("/acompanhamentos/" + id + "/aceitar").with(Login.comoStartup(startupId)));
        mockMvc.perform(post("/acompanhamentos/" + id + "/recusar").with(Login.comoStartup(startupId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta proposta não está mais pendente."));
    }

    @Test
    void mentorNaoPodeAceitarAPropriaProposta() throws Exception {
        long id = proporOk();
        mockMvc.perform(post("/acompanhamentos/" + id + "/aceitar").with(Login.comoMentor(mentorId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startupRecusaEAPropostaSomeDaLista() throws Exception {
        long id = proporOk();
        mockMvc.perform(post("/acompanhamentos/" + id + "/recusar").with(Login.comoStartup(startupId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/acompanhamentos").with(Login.comoStartup(startupId)))
                .andExpect(jsonPath("$.length()").value(0));
        // Depois de recusada, o mentor pode propor de novo
        propor(Login.comoMentor(mentorId), startupId, "Nova tentativa", List.of()).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- quem enxerga o quê

    @Test
    void outraStartupEOutroMentorNaoEnxergam() throws Exception {
        long id = proporOk();
        mockMvc.perform(get("/acompanhamentos/" + id).with(Login.comoStartup(outraStartupId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Acompanhamento não encontrado."));
        mockMvc.perform(get("/acompanhamentos/" + id).with(Login.comoMentor(outroMentorId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/acompanhamentos/" + id + "/aceitar").with(Login.comoStartup(outraStartupId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/acompanhamentos").with(Login.comoStartup(outraStartupId)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cadaLadoVeOsSeusNaLista() throws Exception {
        long id = proporOk();
        mockMvc.perform(get("/acompanhamentos").with(Login.comoMentor(mentorId)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].parceiro.nome").value("Startup do Teste"));
        mockMvc.perform(get("/acompanhamentos").with(Login.comoStartup(startupId)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].parceiro.nome").value("Mentora do Teste"));
    }

    // ---------------------------------------------------------------- apoio

    long proporOk() throws Exception {
        String corpo = propor(Login.comoMentor(mentorId), startupId, "Validar o MVP.", List.of("Meta 1", "Meta 2"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asLong();
    }

    ResultActions propor(RequestPostProcessor quem, Long startup, String objetivo, List<String> metas) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("startupId", startup);
        corpo.put("objetivo", objetivo);
        corpo.put("metas", metas);
        return mockMvc.perform(post("/acompanhamentos").with(quem)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    Long novoMentor(String nome) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setValorSessao(new BigDecimal("100.00"));
        m.setDuracaoSessaoMinutos(60);
        return mentorRepository.save(m).getId();
    }

    Long novaStartup(String nome) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setSegmento("Pet");
        s.setEstagioMaturidade(4);
        return startupRepository.save(s).getId();
    }

    void mentoriaRealizada(Long mentor, Long startup) {
        Agendamento a = agendamento(mentor, startup, LocalDateTime.now(RelogioConfig.FUSO).minusDays(3));
        a.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(a);
    }

    Agendamento agendamento(Long mentor, Long startup, LocalDateTime inicio) {
        Agendamento a = new Agendamento();
        a.setMentorId(mentor);
        a.setStartupId(startup);
        a.setInicio(inicio);
        a.setFim(inicio.plusHours(1));
        a.setValor(new BigDecimal("100.00"));
        a.setStatusPagamento(StatusPagamento.PAGO);
        return a;
    }
}
