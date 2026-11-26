package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Atividade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.StatusAtividade;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AtividadeRepository;
import com.ponti.api.repository.EntregaRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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

// Acompanhamentos, parte 2: atividades do mentor, entregas da startup (enviar, reenviar, desfazer),
// aprovar e devolver, o atraso calculado no servidor e os números do card.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AtividadeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AcompanhamentoRepository acompanhamentoRepository;
    @Autowired AtividadeRepository atividadeRepository;
    @Autowired EntregaRepository entregaRepository;

    Long mentorId;
    Long startupId;
    Long outraStartupId;
    Long acompanhamentoId;
    RequestPostProcessor mentor;
    RequestPostProcessor startup;

    @BeforeEach
    void preparar() {
        mentorId = novoMentor();
        startupId = novaStartup("Startup do Teste");
        outraStartupId = novaStartup("Outra Startup");
        acompanhamentoId = novoAcompanhamento(StatusAcompanhamento.ATIVO);
        mentor = Login.comoMentor(mentorId);
        startup = Login.comoStartup(startupId);
    }

    // ---------------------------------------------------------------- criar atividade

    @Test
    void mentorCriaAtividade() throws Exception {
        criar(mentor, acompanhamentoId, "Mapear 10 entrevistas", daquiDias(7))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Mapear 10 entrevistas"))
                .andExpect(jsonPath("$.acompanhamentoId").value(acompanhamentoId))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.atrasada").value(false))
                .andExpect(jsonPath("$.entregas.length()").value(0));
    }

    @Test
    void naoCriaComPrazoQueJaPassou() throws Exception {
        criar(mentor, acompanhamentoId, "Atividade", LocalDateTime.now(RelogioConfig.FUSO).minusHours(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Escolha um prazo que ainda não passou."));
    }

    @Test
    void naoCriaSemTitulo() throws Exception {
        criar(mentor, acompanhamentoId, " ", daquiDias(3)).andExpect(status().isBadRequest());
    }

    @Test
    void startupNaoCriaAtividade() throws Exception {
        criar(startup, acompanhamentoId, "Atividade", daquiDias(3)).andExpect(status().isForbidden());
    }

    @Test
    void naoCriaEmPropostaNemEmEncerrado() throws Exception {
        Long proposta = novoAcompanhamento(StatusAcompanhamento.PROPOSTO);
        Long encerrado = novoAcompanhamento(StatusAcompanhamento.ENCERRADO);
        criar(mentor, proposta, "Atividade", daquiDias(3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este acompanhamento não está ativo."));
        criar(mentor, encerrado, "Atividade", daquiDias(3)).andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- quem enxerga

    @Test
    void quemNaoParticipaNaoVeNemEntrega() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId + "/atividades").with(Login.comoStartup(outraStartupId)))
                .andExpect(status().isNotFound());
        entregar(Login.comoStartup(outraStartupId), atividade, "Texto", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Atividade não encontrada."));
        mockMvc.perform(post("/atividades/" + atividade + "/aprovar").with(Login.comoMentor(novoMentor())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadaPapelSoFazASuaParte() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));
        entregar(mentor, atividade, "Texto", null).andExpect(status().isForbidden());
        entregar(startup, atividade, "Texto", null).andExpect(status().isOk());
        avaliar(startup, atividade, "aprovar", null).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- fluxo de entrega

    @Test
    void fluxoCompletoEntregaDevolveReenviaAprova() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));

        entregar(startup, atividade, "Primeira versão", "https://notion.so/exemplo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.entregas[0].texto").value("Primeira versão"))
                .andExpect(jsonPath("$.entregas[0].link").value("https://notion.so/exemplo"))
                .andExpect(jsonPath("$.entregas[0].enviadaEm").exists());

        avaliar(mentor, atividade, "devolver", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Explique o que precisa ser ajustado."));
        avaliar(mentor, atividade, "devolver", "Separe os ganhos por segmento.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVOLVIDA"))
                .andExpect(jsonPath("$.entregas[0].feedback").value("Separe os ganhos por segmento."));

        entregar(startup, atividade, "Segunda versão", null)
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.entregas.length()").value(2));

        avaliar(mentor, atividade, "aprovar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.entregas[1].feedback").doesNotExist());

        // Aprovada não aceita mais entrega nem nova avaliação
        entregar(startup, atividade, "Terceira", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta atividade não aceita uma nova entrega agora."));
        avaliar(mentor, atividade, "aprovar", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não há entrega para avaliar."));
    }

    @Test
    void entregaVaziaOuLinkPerigosoSaoRecusados() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));
        entregar(startup, atividade, "  ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Escreva um texto, informe um link ou anexe um arquivo."));
        entregar(startup, atividade, "Texto", "javascript:alert(1)")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe um link que comece com http:// ou https://."));
    }

    @Test
    void desfazerVoltaAoEstadoAnterior() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));

        entregar(startup, atividade, "Primeira", null);
        mockMvc.perform(delete("/atividades/" + atividade + "/entrega").with(startup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.entregas.length()").value(0));

        entregar(startup, atividade, "Primeira", null);
        avaliar(mentor, atividade, "devolver", "Ajuste o texto.");
        entregar(startup, atividade, "Segunda", null);
        mockMvc.perform(delete("/atividades/" + atividade + "/entrega").with(startup))
                .andExpect(jsonPath("$.status").value("DEVOLVIDA"))
                .andExpect(jsonPath("$.entregas.length()").value(1));

        // Depois que o mentor avaliou, não dá mais para desfazer
        mockMvc.perform(delete("/atividades/" + atividade + "/entrega").with(startup))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- atraso e números do card

    @Test
    void atrasoECalculadoPeloPrazo() throws Exception {
        Atividade vencida = salvarAtividade("Vencida", LocalDateTime.now(RelogioConfig.FUSO).minusDays(2), StatusAtividade.PENDENTE);
        Atividade devolvidaVencida = salvarAtividade("Devolvida e vencida", LocalDateTime.now(RelogioConfig.FUSO).minusDays(1), StatusAtividade.DEVOLVIDA);
        salvarAtividade("Aprovada com prazo passado", LocalDateTime.now(RelogioConfig.FUSO).minusDays(5), StatusAtividade.APROVADA);
        salvarAtividade("No prazo", daquiDias(4), StatusAtividade.PENDENTE);

        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId + "/atividades").with(startup))
                .andExpect(status().isOk())
                // Ordem da linha do tempo: do prazo mais antigo ao mais distante
                .andExpect(jsonPath("$[0].titulo").value("Aprovada com prazo passado"))
                .andExpect(jsonPath("$[0].atrasada").value(false))
                .andExpect(jsonPath("$[1].id").value(vencida.getId()))
                .andExpect(jsonPath("$[1].atrasada").value(true))
                .andExpect(jsonPath("$[2].id").value(devolvidaVencida.getId()))
                .andExpect(jsonPath("$[2].status").value("DEVOLVIDA"))
                .andExpect(jsonPath("$[2].atrasada").value(true))
                .andExpect(jsonPath("$[3].atrasada").value(false));

        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId).with(startup))
                .andExpect(jsonPath("$.atividadesAtrasadas").value(2));

        // Entregou (mesmo atrasada): deixa de contar como atrasada
        entregar(startup, vencida.getId(), "Entreguei atrasado", null)
                .andExpect(jsonPath("$.atrasada").value(false));
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId).with(startup))
                .andExpect(jsonPath("$.atividadesAtrasadas").value(1));
    }

    @Test
    void numerosDoCardVemDasAtividadesGuardadas() throws Exception {
        LocalDateTime perto = daquiDias(2);
        long a1 = criarOk("Uma", perto);
        long a2 = criarOk("Duas", daquiDias(5));
        criarOk("Três", daquiDias(9));
        entregar(startup, a1, "Feito", null);
        avaliar(mentor, a1, "aprovar", "Muito bom!");
        entregar(startup, a2, "Feito", null);

        // Startup: 1 aprovada de 3; a entregar só a "Três" (a "Duas" espera o mentor)
        mockMvc.perform(get("/acompanhamentos").with(startup))
                .andExpect(jsonPath("$[0].atividadesAprovadas").value(1))
                .andExpect(jsonPath("$[0].atividadesTotal").value(3))
                .andExpect(jsonPath("$[0].pendencias").value(1))
                .andExpect(jsonPath("$[0].proximoPrazo").value(daquiDias(9).toLocalDate().toString()))
                .andExpect(jsonPath("$[0].atividadesAtrasadas").value(0));

        // Mentor: 1 entrega esperando revisão (a "Duas")
        mockMvc.perform(get("/acompanhamentos/" + acompanhamentoId).with(mentor))
                .andExpect(jsonPath("$.pendencias").value(1))
                .andExpect(jsonPath("$.atividadesAprovadas").value(1));
    }

    // ---------------------------------------------------------------- exclusão de conta (LGPD)

    @Test
    void excluirAContaApagaOsAcompanhamentosETudoDentroDeles() throws Exception {
        long atividade = criarOk("Atividade", daquiDias(3));
        entregar(startup, atividade, "Entrega", null);

        mockMvc.perform(delete("/startups/" + startupId).with(startup)).andExpect(status().isNoContent());

        assertThat(acompanhamentoRepository.findByStartupId(startupId)).isEmpty();
        assertThat(atividadeRepository.findById(atividade)).isEmpty();
        assertThat(entregaRepository.findByAtividadeIdOrderByIdAsc(atividade)).isEmpty();
    }

    @Test
    void excluirAContaDoMentorTambemApaga() throws Exception {
        criarOk("Atividade", daquiDias(3));
        mockMvc.perform(delete("/mentores/" + mentorId).with(mentor)).andExpect(status().isNoContent());
        assertThat(acompanhamentoRepository.findByMentorId(mentorId)).isEmpty();
    }

    // ---------------------------------------------------------------- apoio

    LocalDateTime daquiDias(int dias) {
        return LocalDateTime.now(RelogioConfig.FUSO).plusDays(dias).withHour(23).withMinute(59).truncatedTo(ChronoUnit.MINUTES);
    }

    long criarOk(String titulo, LocalDateTime prazo) throws Exception {
        String corpo = criar(mentor, acompanhamentoId, titulo, prazo)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asLong();
    }

    ResultActions criar(RequestPostProcessor quem, Long acompanhamento, String titulo, LocalDateTime prazo) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("descricao", "Descrição da atividade.");
        corpo.put("prazo", prazo.toString());
        return mockMvc.perform(post("/acompanhamentos/" + acompanhamento + "/atividades").with(quem)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
    }

    ResultActions entregar(RequestPostProcessor quem, long atividade, String texto, String link) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("texto", texto);
        corpo.put("link", link);
        return mockMvc.perform(post("/atividades/" + atividade + "/entrega").with(quem)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
    }

    ResultActions avaliar(RequestPostProcessor quem, long atividade, String acao, String feedback) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("feedback", feedback);
        return mockMvc.perform(post("/atividades/" + atividade + "/" + acao).with(quem)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
    }

    Atividade salvarAtividade(String titulo, LocalDateTime prazo, StatusAtividade status) {
        Atividade a = new Atividade();
        a.setAcompanhamentoId(acompanhamentoId);
        a.setTitulo(titulo);
        a.setDescricao("");
        a.setPrazo(prazo);
        a.setStatus(status);
        return atividadeRepository.save(a);
    }

    Long novoAcompanhamento(StatusAcompanhamento status) {
        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentorId);
        a.setStartupId(startupId);
        a.setObjetivo("Validar a proposta de valor.");
        a.setStatus(status);
        if (status != StatusAcompanhamento.PROPOSTO) a.setDataInicio(LocalDate.now(RelogioConfig.FUSO).minusDays(30));
        return acompanhamentoRepository.save(a).getId();
    }

    Long novoMentor() {
        Mentor m = new Mentor();
        m.setNome("Mentora do Teste");
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
