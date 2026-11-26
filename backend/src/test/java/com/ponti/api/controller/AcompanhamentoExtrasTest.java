package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Atividade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Meta;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AtividadeRepository;
import com.ponti.api.repository.ComentarioRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.MetaRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

// Acompanhamentos, parte 3: comentários nas entregas, painel consolidado, metas e encerramento.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcompanhamentoExtrasTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AcompanhamentoRepository acompanhamentoRepository;
    @Autowired AtividadeRepository atividadeRepository;
    @Autowired MetaRepository metaRepository;
    @Autowired ComentarioRepository comentarioRepository;

    Long mentorId;
    Long startupId;
    Long acompanhamentoId;
    RequestPostProcessor mentor;
    RequestPostProcessor startup;

    @BeforeEach
    void preparar() {
        mentorId = novoMentor("Juliana Mentora");
        startupId = novaStartup("PetCare");
        acompanhamentoId = novoAcompanhamento(mentorId, startupId, StatusAcompanhamento.ATIVO);
        mentor = Login.comoMentor(mentorId);
        startup = Login.comoStartup(startupId);
    }

    // ---------------------------------------------------------------- comentários

    @Test
    void osDoisLadosComentamNaEntrega() throws Exception {
        long atividade = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long entrega = entregarERetornarId(atividade);

        comentar(mentor, atividade, entrega, "Podemos conversar sobre o roteiro?").andExpect(status().isOk());
        comentar(startup, atividade, entrega, "  Pode ser!  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entregas[0].comentarios.length()").value(2))
                .andExpect(jsonPath("$.entregas[0].comentarios[0].autor").value("MENTOR"))
                .andExpect(jsonPath("$.entregas[0].comentarios[0].nomeAutor").value("Juliana Mentora"))
                .andExpect(jsonPath("$.entregas[0].comentarios[1].autor").value("STARTUP"))
                .andExpect(jsonPath("$.entregas[0].comentarios[1].nomeAutor").value("PetCare"))
                .andExpect(jsonPath("$.entregas[0].comentarios[1].texto").value("Pode ser!"))
                .andExpect(jsonPath("$.entregas[0].comentarios[1].criadoEm").exists());
    }

    @Test
    void comentarioInvalidoOuDeFora() throws Exception {
        long atividade = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long outraAtividade = novaAtividade(acompanhamentoId, agora().plusDays(4)).getId();
        long entrega = entregarERetornarId(atividade);

        comentar(mentor, atividade, entrega, " ").andExpect(status().isBadRequest());
        // Entrega que não é desta atividade
        comentar(mentor, outraAtividade, entrega, "Oi")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Entrega não encontrada."));
        // Quem não participa
        comentar(Login.comoStartup(novaStartup("Intrusa")), atividade, entrega, "Oi").andExpect(status().isNotFound());
    }

    @Test
    void desfazerEntregaApagaOsComentariosDela() throws Exception {
        long atividade = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long entrega = entregarERetornarId(atividade);
        comentar(startup, atividade, entrega, "Comentário").andExpect(status().isOk());

        mockMvc.perform(delete("/atividades/" + atividade + "/entrega").with(startup)).andExpect(status().isOk());
        assertThat(comentarioRepository.findByEntregaIdOrderByIdAsc(entrega)).isEmpty();
    }

    // ---------------------------------------------------------------- painel

    @Test
    void painelSemEntregasMostraNuloNoPrazo() throws Exception {
        novaAtividade(acompanhamentoId, agora().plusDays(3));
        mockMvc.perform(get("/acompanhamentos/painel").with(startup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acompanhamentosAtivos").value(1))
                .andExpect(jsonPath("$.atividadesTotal").value(1))
                .andExpect(jsonPath("$.entregasNoPrazo").doesNotExist());
    }

    @Test
    void painelSomaSoOsAtivosECalculaEntregasNoPrazo() throws Exception {
        // Segundo acompanhamento ativo, com outro mentor
        Long outroMentor = novoMentor("Ricardo");
        Long segundo = novoAcompanhamento(outroMentor, startupId, StatusAcompanhamento.ATIVO);
        // Encerrado não entra no painel
        Long encerrado = novoAcompanhamento(mentorId, startupId, StatusAcompanhamento.ENCERRADO);
        novaAtividade(encerrado, agora().minusDays(10));

        long noPrazo = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long atrasada = novaAtividade(segundo, agora().minusDays(1)).getId();
        novaAtividade(segundo, agora().plusDays(5));
        entregar(startup, noPrazo);
        entregar(startup, atrasada);
        avaliar(mentor, noPrazo, "aprovar");

        meta(acompanhamentoId, "Meta A", true);
        meta(segundo, "Meta B", false);
        meta(encerrado, "Meta do encerrado", true);

        mockMvc.perform(get("/acompanhamentos/painel").with(startup))
                .andExpect(jsonPath("$.acompanhamentosAtivos").value(2))
                .andExpect(jsonPath("$.atividadesTotal").value(3))
                .andExpect(jsonPath("$.atividadesAprovadas").value(1))
                .andExpect(jsonPath("$.entregasNoPrazo").value(50))
                .andExpect(jsonPath("$.metasCumpridas").value(1))
                .andExpect(jsonPath("$.metasTotal").value(2));

        // O mentor vê só o que é dele
        mockMvc.perform(get("/acompanhamentos/painel").with(mentor))
                .andExpect(jsonPath("$.acompanhamentosAtivos").value(1))
                .andExpect(jsonPath("$.atividadesTotal").value(1))
                .andExpect(jsonPath("$.entregasNoPrazo").value(100));
    }

    // ---------------------------------------------------------------- metas

    @Test
    void mentorMarcaMetaComoCumprida() throws Exception {
        Meta m1 = meta(acompanhamentoId, "Fechar 3 pilotos", false);
        meta(acompanhamentoId, "Definir preço", false);

        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId + "/metas").with(startup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("Fechar 3 pilotos"))
                .andExpect(jsonPath("$[0].cumprida").value(false));

        marcar(mentor, m1.getId(), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cumprida").value(true))
                .andExpect(jsonPath("$.cumpridaEm").exists());
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId).with(startup))
                .andExpect(jsonPath("$.metasCumpridas").value(1))
                .andExpect(jsonPath("$.metasTotal").value(2));

        marcar(mentor, m1.getId(), false)
                .andExpect(jsonPath("$.cumprida").value(false))
                .andExpect(jsonPath("$.cumpridaEm").doesNotExist());
    }

    @Test
    void soOMentorDoAcompanhamentoMarcaMeta() throws Exception {
        Meta m = meta(acompanhamentoId, "Meta", false);
        marcar(startup, m.getId(), true).andExpect(status().isForbidden());
        marcar(Login.comoMentor(novoMentor("Outro")), m.getId(), true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meta não encontrada."));
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId + "/metas").with(Login.comoStartup(novaStartup("Outra"))))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- encerrar

    @Test
    void mentorEncerraETudoViraSomenteLeitura() throws Exception {
        long atividade = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long entrega = entregarERetornarId(atividade);
        Meta m = meta(acompanhamentoId, "Meta", false);

        mockMvc.perform(post("/acompanhamentos/" + acompanhamentoId + "/encerrar").with(mentor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADO"))
                .andExpect(jsonPath("$.dataFim").value(LocalDate.now(RelogioConfig.FUSO).toString()));

        // Continua aparecendo na lista e dá para consultar as atividades
        mockMvc.perform(get("/acompanhamentos").with(startup)).andExpect(jsonPath("$[0].status").value("ENCERRADO"));
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId + "/atividades").with(startup)).andExpect(status().isOk());

        // Mas nada pode ser alterado
        comentar(startup, atividade, entrega, "Oi").andExpect(status().isConflict());
        avaliar(mentor, atividade, "aprovar").andExpect(status().isConflict());
        marcar(mentor, m.getId(), true).andExpect(status().isConflict());
        mockMvc.perform(post("/acompanhamentos/" + acompanhamentoId + "/encerrar").with(mentor))
                .andExpect(status().isConflict());
    }

    @Test
    void startupNaoEncerraENemPropostaPodeSerEncerrada() throws Exception {
        mockMvc.perform(post("/acompanhamentos/" + acompanhamentoId + "/encerrar").with(startup))
                .andExpect(status().isForbidden());
        Long proposta = novoAcompanhamento(mentorId, novaStartup("Nova"), StatusAcompanhamento.PROPOSTO);
        mockMvc.perform(post("/acompanhamentos/" + proposta + "/encerrar").with(mentor))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- LGPD

    @Test
    void excluirAContaApagaTambemOsComentarios() throws Exception {
        long atividade = novaAtividade(acompanhamentoId, agora().plusDays(3)).getId();
        long entrega = entregarERetornarId(atividade);
        comentar(mentor, atividade, entrega, "Comentário").andExpect(status().isOk());
        meta(acompanhamentoId, "Meta", false);

        mockMvc.perform(delete("/startups/" + startupId).with(startup)).andExpect(status().isNoContent());
        assertThat(comentarioRepository.findByEntregaIdOrderByIdAsc(entrega)).isEmpty();
        assertThat(metaRepository.findByAcompanhamentoIdOrderByOrdemAsc(acompanhamentoId)).isEmpty();
    }

    // ---------------------------------------------------------------- apoio

    LocalDateTime agora() {
        return LocalDateTime.now(RelogioConfig.FUSO).withSecond(0).withNano(0);
    }

    ResultActions comentar(RequestPostProcessor quem, long atividade, long entrega, String texto) throws Exception {
        return mockMvc.perform(post("/atividades/" + atividade + "/entregas/" + entrega + "/comentarios").with(quem)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("texto", texto))));
    }

    ResultActions entregar(RequestPostProcessor quem, long atividade) throws Exception {
        return mockMvc.perform(post("/atividades/" + atividade + "/entrega").with(quem)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Entrega\"}"));
    }

    long entregarERetornarId(long atividade) throws Exception {
        String corpo = entregar(startup, atividade).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("entregas").get(0).get("id").asLong();
    }

    ResultActions avaliar(RequestPostProcessor quem, long atividade, String acao) throws Exception {
        return mockMvc.perform(post("/atividades/" + atividade + "/" + acao).with(quem)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    ResultActions marcar(RequestPostProcessor quem, long meta, boolean cumprida) throws Exception {
        return mockMvc.perform(patch("/metas/" + meta).with(quem)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cumprida\":" + cumprida + "}"));
    }

    Atividade novaAtividade(Long acompanhamento, LocalDateTime prazo) {
        Atividade a = new Atividade();
        a.setAcompanhamentoId(acompanhamento);
        a.setTitulo("Atividade");
        a.setDescricao("");
        a.setPrazo(prazo);
        return atividadeRepository.save(a);
    }

    Meta meta(Long acompanhamento, String titulo, boolean cumprida) {
        Meta m = new Meta();
        m.setAcompanhamentoId(acompanhamento);
        m.setTitulo(titulo);
        m.setCumprida(cumprida);
        m.setOrdem((int) metaRepository.count());
        return metaRepository.save(m);
    }

    Long novoAcompanhamento(Long mentor, Long startup, StatusAcompanhamento status) {
        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentor);
        a.setStartupId(startup);
        a.setObjetivo("Objetivo do teste.");
        a.setStatus(status);
        if (status != StatusAcompanhamento.PROPOSTO) a.setDataInicio(LocalDate.now(RelogioConfig.FUSO).minusDays(30));
        return acompanhamentoRepository.save(a).getId();
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
        return startupRepository.save(s).getId();
    }
}
