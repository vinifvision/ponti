package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.dto.NovaAvaliacao;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.MentorIndicadores;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Usuario;
import com.ponti.api.pagamento.GatewayPagamentoSimulado;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.MentorIndicadoresRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.AvaliacaoService;
import com.ponti.api.service.ContaService;
import com.ponti.api.service.IndicadoresMentorService;
import com.ponti.api.service.RankingMentores.Indicadores;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Tabela MentorIndicadores (etapa 4 do documento de busca e ordenação): a busca lê os indicadores
// pré-calculados, e cada mudança relevante recalcula na hora só o mentor afetado.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IndicadoresMentorTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AgendamentoRepository agendamentoRepository;
    @Autowired MentorIndicadoresRepository indicadoresRepository;
    @Autowired IndicadoresMentorService indicadoresService;
    @Autowired AvaliacaoService avaliacaoService;
    @Autowired ContaService contaService;
    @Autowired GatewayPagamentoSimulado gateway;

    Long mentorId;
    Long startupId;
    // A próxima terça (de 1 a 7 dias à frente): sempre dentro da janela de 14 dias e da semana
    LocalDate terca;

    @BeforeEach
    void preparar() throws Exception {
        Mentor m = new Mentor();
        m.setNome("Mentor dos Indicadores");
        m.setValorSessao(new BigDecimal("150.00"));
        m.setDuracaoSessaoMinutos(60);
        mentorId = mentorRepository.save(m).getId();
        Startup s = new Startup();
        s.setNome("Startup dos Indicadores");
        startupId = startupRepository.save(s).getId();
        terca = LocalDate.now(RelogioConfig.FUSO).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

        definirDisponibilidade(List.of(Map.of("diaSemana", "TUESDAY", "horaInicio", "09:00", "horaFim", "12:00")))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- apoio

    MentorIndicadores linha() {
        return indicadoresRepository.findById(mentorId).orElseThrow();
    }

    ResultActions enviarJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    ResultActions definirDisponibilidade(List<Map<String, Object>> faixas) throws Exception {
        return enviarJson(put("/mentores/" + mentorId + "/disponibilidade").with(Login.comoMentor(mentorId)), faixas);
    }

    Long agendar(LocalDateTime inicio, String metodo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("startupId", startupId);
        body.put("mentorId", mentorId);
        body.put("inicio", inicio.toString());
        body.put("metodoPagamento", metodo);
        if ("CARTAO".equals(metodo)) {
            body.put("cartao", Map.of("numero", "4242 4242 4242 4242", "nomeImpresso", "FULANO DA SILVA",
                    "validade", "12/35", "cvv", "123"));
        }
        String json = enviarJson(post("/agendamentos").with(Login.comoStartup(startupId)), body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    Indicadores indicadoresNaBusca() {
        return indicadoresService.coletar(startupId, Map.of()).indicadores().stream()
                .filter(i -> i.mentorId().equals(mentorId)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- recálculo por evento

    @Nested
    class RecalculoPorEvento {

        @Test
        void definirAgendaCriaALinhaComHorariosLivres() {
            MentorIndicadores linha = linha();
            assertThat(linha.isTemHorario7d()).isTrue();
            assertThat(linha.isTemHorario14d()).isTrue();
            assertThat(linha.getOcupacao()).isEqualTo(0.0);
            assertThat(linha.getAtualizadoEm()).isNotNull();
        }

        @Test
        void agendaVaziaTiraOsHorariosEAOcupacao() throws Exception {
            definirDisponibilidade(List.of()).andExpect(status().isOk());

            assertThat(linha().isTemHorario14d()).isFalse();
            assertThat(linha().getOcupacao()).isNull();
        }

        @Test
        void pedidoPagoNoCartaoEntraNaOcupacaoNaHora() throws Exception {
            agendar(terca.atTime(9, 0), "CARTAO");

            assertThat(linha().getOcupacao()).isGreaterThan(0.0);
        }

        @Test
        void pixSoEntraNaOcupacaoDepoisDePago() throws Exception {
            Long id = agendar(terca.atTime(9, 0), "PIX");
            assertThat(linha().getOcupacao()).isEqualTo(0.0); // aguardando pagamento: ainda não reservado

            gateway.confirmarPix(agendamentoRepository.findById(id).orElseThrow());

            assertThat(linha().getOcupacao()).isGreaterThan(0.0);
        }

        @Test
        void recusarRegistraOTempoDeRespostaEDerrubaAConfiabilidade() throws Exception {
            Long id = agendar(terca.atTime(9, 0), "CARTAO");
            assertThat(linha().getTaxaConfiabilidade()).isEqualTo(1.0);

            mockMvc.perform(post("/agendamentos/" + id + "/recusar").with(Login.comoMentor(mentorId)))
                    .andExpect(status().isOk());

            assertThat(linha().getPedidosRespondidos()).isEqualTo(1);
            assertThat(linha().getTempoRespostaMedianoHoras()).isNotNull();
            assertThat(linha().getTaxaConfiabilidade()).isEqualTo(0.0);
            assertThat(linha().getOcupacao()).isEqualTo(0.0); // o horário recusado voltou a ficar livre
        }

        @Test
        void cancelamentoPelaStartupLiberaOHorarioSemPunirOMentor() throws Exception {
            Long id = agendar(terca.atTime(9, 0), "CARTAO");

            mockMvc.perform(post("/agendamentos/" + id + "/cancelar").with(Login.comoStartup(startupId)))
                    .andExpect(status().isOk());

            assertThat(linha().getOcupacao()).isEqualTo(0.0);
            assertThat(linha().getTaxaConfiabilidade()).isEqualTo(1.0);
        }

        @Test
        void avaliacaoAtualizaANotaDoMentor() {
            Agendamento a = new Agendamento();
            a.setStartupId(startupId);
            a.setMentorId(mentorId);
            a.setInicio(LocalDateTime.now(RelogioConfig.FUSO).minusDays(3));
            a.setFim(a.getInicio().plusHours(1));
            a.setValor(BigDecimal.ZERO);
            a.setStatus(StatusAgendamento.CONFIRMADO);
            a.setStatusPagamento(StatusPagamento.GRATUITA);
            a = agendamentoRepository.save(a);

            avaliacaoService.avaliar(a, new NovaAvaliacao(5, "Ótima"));

            assertThat(linha().getTotalAvaliacoes()).isEqualTo(1);
            assertThat(linha().getSomaNotas()).isEqualTo(5.0);
            assertThat(linha().getMentoriasRealizadas()).isEqualTo(1);
        }

        @Test
        void cadastroDeMentorJaCriaALinha() throws Exception {
            mockMvc.perform(post("/auth/cadastro/mentor").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(AuthControllerTest.mentorValido())))
                    .andExpect(status().isCreated());

            Long novo = usuarioRepository.findByEmail("maria@email.com").map(Usuario::getMentorId).orElseThrow();
            MentorIndicadores linha = indicadoresRepository.findById(novo).orElseThrow();
            assertThat(linha.isTemHorario14d()).isFalse(); // ainda não definiu a agenda
        }

        @Test
        void excluirAContaDoMentorApagaALinha() {
            contaService.excluirMentor(mentorId);

            assertThat(indicadoresRepository.findById(mentorId)).isEmpty();
        }
    }

    // ---------------------------------------------------------------- recálculo periódico e busca

    @Nested
    class RecalculoPeriodicoEBusca {

        @Test
        void recalcularTodosCorrigeLinhaDesatualizadaEApagaAsOrfas() {
            MentorIndicadores velha = linha();
            velha.setTemHorario14d(false);
            velha.setAtualizadoEm(Instant.parse("2020-01-01T00:00:00Z"));
            indicadoresRepository.save(velha);
            MentorIndicadores orfa = new MentorIndicadores();
            orfa.setMentorId(987654L); // mentor que não existe
            indicadoresRepository.save(orfa);

            indicadoresService.recalcularTodos();

            assertThat(linha().isTemHorario14d()).isTrue();
            assertThat(linha().getAtualizadoEm()).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
            assertThat(indicadoresRepository.findById(987654L)).isEmpty();
        }

        @Test
        void aBuscaLeDaTabela() {
            // A tabela diz "sem horário" (por exemplo, antes do próximo recálculo): a busca confia nela
            MentorIndicadores linha = linha();
            linha.setTemHorario14d(false);
            indicadoresRepository.save(linha);

            assertThat(indicadoresNaBusca().temHorario14d()).isFalse();
        }

        @Test
        void mentorSemLinhaECalculadoNaHoraSemGravar() {
            indicadoresRepository.deleteById(mentorId);

            assertThat(indicadoresNaBusca().temHorario14d()).isTrue();
            assertThat(indicadoresRepository.findById(mentorId)).isEmpty();
        }
    }
}
