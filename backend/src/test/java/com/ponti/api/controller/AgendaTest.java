package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Convite;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusConvite;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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

// Agenda estilo Preply: disponibilidade do mentor, horários livres, agendamento com pagamento
// simulado e as respostas do mentor (confirmar com link, recusar) e o cancelamento.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgendaTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired ConviteRepository conviteRepository;

    Long mentorId;
    Long startupId;
    // Uma terça-feira daqui a mais de uma semana, para os horários estarem sempre no futuro
    LocalDate terca;

    @BeforeEach
    void preparar() throws Exception {
        mentorId = novoMentor(new BigDecimal("150.00"), 60);
        Startup s = new Startup();
        s.setNome("Startup da Agenda");
        startupId = startupRepository.save(s).getId();
        terca = LocalDate.now(RelogioConfig.FUSO).with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);

        definirDisponibilidade(mentorId, List.of(faixa("TUESDAY", "09:00", "12:00")))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- apoio

    Long novoMentor(BigDecimal valor, Integer duracao) {
        Mentor m = new Mentor();
        m.setNome("Mentor da Agenda");
        m.setValorSessao(valor);
        m.setDuracaoSessaoMinutos(duracao);
        return mentorRepository.save(m).getId();
    }

    Map<String, Object> faixa(String dia, String inicio, String fim) {
        return Map.of("diaSemana", dia, "horaInicio", inicio, "horaFim", fim);
    }

    ResultActions enviarJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    ResultActions definirDisponibilidade(Long mentor, List<Map<String, Object>> faixas) throws Exception {
        return enviarJson(put("/mentores/" + mentor + "/disponibilidade"), faixas);
    }

    List<String> horarios(Long mentor, LocalDate de, LocalDate ate) throws Exception {
        String json = mockMvc.perform(get("/mentores/" + mentor + "/horarios")
                        .param("de", de.toString()).param("ate", ate.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> lista = new ArrayList<>();
        objectMapper.readTree(json).forEach(n -> lista.add(n.asText()));
        return lista;
    }

    Map<String, Object> pedido(LocalDateTime inicio, String metodo) {
        Map<String, Object> body = new HashMap<>();
        body.put("startupId", startupId);
        body.put("mentorId", mentorId);
        body.put("inicio", inicio.toString());
        if (metodo != null) {
            body.put("metodoPagamento", metodo);
        }
        body.put("mensagem", "Queremos validar o MVP");
        return body;
    }

    Long agendar(LocalDateTime inicio) throws Exception {
        String json = enviarJson(post("/agendamentos"), pedido(inicio, "PIX"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    // ---------------------------------------------------------------- testes

    @Nested
    class Disponibilidade {

        @Test
        void guardaEDevolveAsFaixasDaSemana() throws Exception {
            definirDisponibilidade(mentorId, List.of(
                    faixa("THURSDAY", "14:00", "16:00"),
                    faixa("TUESDAY", "09:00", "12:00")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/mentores/" + mentorId + "/disponibilidade"))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].diaSemana").value("TUESDAY"))
                    .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                    .andExpect(jsonPath("$[1].diaSemana").value("THURSDAY"));
        }

        @Test
        void recusaFaixaQueTerminaAntesDeComecar() throws Exception {
            definirDisponibilidade(mentorId, List.of(faixa("MONDAY", "12:00", "09:00")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void recusaFaixasSobrepostasNoMesmoDia() throws Exception {
            definirDisponibilidade(mentorId, List.of(
                    faixa("MONDAY", "09:00", "12:00"),
                    faixa("MONDAY", "11:00", "13:00")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void listaVaziaDeixaOMentorSemHorarios() throws Exception {
            definirDisponibilidade(mentorId, List.of()).andExpect(status().isOk());

            assertThat(horarios(mentorId, terca, terca)).isEmpty();
        }
    }

    @Nested
    class HorariosLivres {

        @Test
        void geraBlocosDoTamanhoDaSessao() throws Exception {
            assertThat(horarios(mentorId, terca, terca)).containsExactly(
                    terca + "T09:00:00", terca + "T10:00:00", terca + "T11:00:00");
        }

        @Test
        void sessaoDe45MinutosNaoPassaDoFimDaFaixa() throws Exception {
            Long outro = novoMentor(BigDecimal.ZERO, 45);
            definirDisponibilidade(outro, List.of(faixa("TUESDAY", "09:00", "11:00"))).andExpect(status().isOk());

            // 09:00, 09:45 e 10:30 terminaria 11:15, depois do fim da faixa
            assertThat(horarios(outro, terca, terca)).containsExactly(terca + "T09:00:00", terca + "T09:45:00");
        }

        @Test
        void soMostraOsDiasDaSemanaCertos() throws Exception {
            LocalDate quarta = terca.plusDays(1);
            assertThat(horarios(mentorId, quarta, quarta)).isEmpty();
            assertThat(horarios(mentorId, terca, terca.plusDays(6))).hasSize(3);
        }

        @Test
        void horarioAgendadoSaiDaLista() throws Exception {
            agendar(terca.atTime(10, 0));

            assertThat(horarios(mentorId, terca, terca)).containsExactly(terca + "T09:00:00", terca + "T11:00:00");
        }

        @Test
        void horarioRecusadoVoltaParaALista() throws Exception {
            Long id = agendar(terca.atTime(10, 0));
            mockMvc.perform(post("/agendamentos/" + id + "/recusar")).andExpect(status().isOk());

            assertThat(horarios(mentorId, terca, terca)).contains(terca + "T10:00:00");
        }

        @Test
        void naoMostraHorariosQueJaPassaram() throws Exception {
            LocalDate tercaPassada = LocalDate.now(RelogioConfig.FUSO).with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));
            assertThat(horarios(mentorId, tercaPassada, tercaPassada)).isEmpty();
        }

        @Test
        void recusaPeriodoInvertidoOuLongoDemais() throws Exception {
            mockMvc.perform(get("/mentores/" + mentorId + "/horarios")
                            .param("de", terca.toString()).param("ate", terca.minusDays(1).toString()))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/mentores/" + mentorId + "/horarios")
                            .param("de", terca.toString()).param("ate", terca.plusDays(90).toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Agendar {

        @Test
        void mentoriaPagaFicaPendenteEPaga() throws Exception {
            enviarJson(post("/agendamentos"), pedido(terca.atTime(9, 0), "CARTAO"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                    .andExpect(jsonPath("$.metodoPagamento").value("CARTAO"))
                    .andExpect(jsonPath("$.valor").value(150.0))
                    .andExpect(jsonPath("$.fim").value(terca + "T10:00:00"))
                    .andExpect(jsonPath("$.mensagem").value("Queremos validar o MVP"))
                    .andExpect(jsonPath("$.mentor.nome").value("Mentor da Agenda"))
                    .andExpect(jsonPath("$.startup.nome").value("Startup da Agenda"));
        }

        @Test
        void mentoriaPagaSemFormaDePagamentoDa400() throws Exception {
            enviarJson(post("/agendamentos"), pedido(terca.atTime(9, 0), null))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void mentoriaGratuitaNaoPrecisaDePagamento() throws Exception {
            Long gratuito = novoMentor(BigDecimal.ZERO, 60);
            definirDisponibilidade(gratuito, List.of(faixa("TUESDAY", "09:00", "10:00"))).andExpect(status().isOk());
            Map<String, Object> body = pedido(terca.atTime(9, 0), null);
            body.put("mentorId", gratuito);

            enviarJson(post("/agendamentos"), body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusPagamento").value("GRATUITA"))
                    .andExpect(jsonPath("$.metodoPagamento").doesNotExist());
        }

        @Test
        void naoAgendaOMesmoHorarioDuasVezes() throws Exception {
            agendar(terca.atTime(9, 0));

            enviarJson(post("/agendamentos"), pedido(terca.atTime(9, 0), "PIX"))
                    .andExpect(status().isConflict());
        }

        @Test
        void naoAgendaForaDaDisponibilidade() throws Exception {
            enviarJson(post("/agendamentos"), pedido(terca.atTime(15, 0), "PIX"))
                    .andExpect(status().isConflict());
            enviarJson(post("/agendamentos"), pedido(terca.atTime(9, 30), "PIX"))
                    .andExpect(status().isConflict());
        }

        @Test
        void naoAgendaNoPassado() throws Exception {
            LocalDate tercaPassada = LocalDate.now(RelogioConfig.FUSO).with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));
            enviarJson(post("/agendamentos"), pedido(tercaPassada.atTime(9, 0), "PIX"))
                    .andExpect(status().isConflict());
        }

        @Test
        void agendarRespondeASugestaoDoMentor() throws Exception {
            Convite sugestao = new Convite();
            sugestao.setStartupId(startupId);
            sugestao.setMentorId(mentorId);
            sugestao.setEnviadoPor(TipoUsuario.MENTOR);
            Long sugestaoId = conviteRepository.save(sugestao).getId();

            agendar(terca.atTime(9, 0));

            assertThat(conviteRepository.findById(sugestaoId).orElseThrow().getStatus()).isEqualTo(StatusConvite.ACEITO);
        }
    }

    @Nested
    class RespostasDoMentor {

        @Test
        void confirmarGuardaOLinkDaReuniao() throws Exception {
            Long id = agendar(terca.atTime(9, 0));

            enviarJson(post("/agendamentos/" + id + "/confirmar"), Map.of("linkReuniao", "https://meet.google.com/abc-defg-hij"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                    .andExpect(jsonPath("$.linkReuniao").value("https://meet.google.com/abc-defg-hij"));

            JsonNode lista = objectMapper.readTree(mockMvc.perform(get("/agendamentos").param("startupId", startupId.toString()))
                    .andReturn().getResponse().getContentAsString());
            assertThat(lista.get(0).get("linkReuniao").asText()).isEqualTo("https://meet.google.com/abc-defg-hij");
        }

        @Test
        void confirmarExigeLinkHttps() throws Exception {
            Long id = agendar(terca.atTime(9, 0));

            enviarJson(post("/agendamentos/" + id + "/confirmar"), Map.of("linkReuniao", "meet.google.com/abc"))
                    .andExpect(status().isBadRequest());
            enviarJson(post("/agendamentos/" + id + "/confirmar"), Map.of("linkReuniao", "javascript:alert(1)"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void naoConfirmaDuasVezes() throws Exception {
            Long id = agendar(terca.atTime(9, 0));
            Map<String, String> link = Map.of("linkReuniao", "https://meet.google.com/abc-defg-hij");
            enviarJson(post("/agendamentos/" + id + "/confirmar"), link).andExpect(status().isOk());

            enviarJson(post("/agendamentos/" + id + "/confirmar"), link).andExpect(status().isConflict());
            mockMvc.perform(post("/agendamentos/" + id + "/recusar")).andExpect(status().isConflict());
        }

        @Test
        void recusarReembolsaOPagamento() throws Exception {
            Long id = agendar(terca.atTime(9, 0));

            mockMvc.perform(post("/agendamentos/" + id + "/recusar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECUSADO"))
                    .andExpect(jsonPath("$.statusPagamento").value("REEMBOLSADO"));
        }

        @Test
        void cancelarSessaoConfirmadaReembolsaELiberaOHorario() throws Exception {
            Long id = agendar(terca.atTime(9, 0));
            enviarJson(post("/agendamentos/" + id + "/confirmar"), Map.of("linkReuniao", "https://meet.google.com/x"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/agendamentos/" + id + "/cancelar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"))
                    .andExpect(jsonPath("$.statusPagamento").value("REEMBOLSADO"))
                    .andExpect(jsonPath("$.linkReuniao").doesNotExist());

            assertThat(horarios(mentorId, terca, terca)).contains(terca + "T09:00:00");
            mockMvc.perform(post("/agendamentos/" + id + "/cancelar")).andExpect(status().isConflict());
        }

        @Test
        void agendamentoInexistenteDa404() throws Exception {
            mockMvc.perform(post("/agendamentos/999999/recusar")).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Listagem {

        @Test
        void listaPorStartupEPorMentorEmOrdemDeData() throws Exception {
            agendar(terca.atTime(11, 0));
            agendar(terca.atTime(9, 0));

            mockMvc.perform(get("/agendamentos").param("mentorId", mentorId.toString()))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].inicio").value(terca + "T09:00:00"))
                    .andExpect(jsonPath("$[1].inicio").value(terca + "T11:00:00"));
            mockMvc.perform(get("/agendamentos").param("startupId", startupId.toString()))
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void exigeStartupOuMentor() throws Exception {
            mockMvc.perform(get("/agendamentos")).andExpect(status().isBadRequest());
        }

        @Test
        void linkSoApareceQuandoConfirmado() throws Exception {
            agendar(terca.atTime(9, 0));

            mockMvc.perform(get("/agendamentos").param("mentorId", mentorId.toString()))
                    .andExpect(jsonPath("$[0].linkReuniao").doesNotExist());
        }
    }
}
