package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.pagamento.GatewayPagamentoSimulado;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.DayOfWeek;
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
import org.springframework.transaction.annotation.Transactional;

// Pagamento simulado: Pix (aguardando → pago ou expirado) e cartão (aprovado, recusado, inválido).
// Aqui o "banco" confirma o Pix na hora (aprovação em 0 segundos), para testar a virada de status.
@SpringBootTest(properties = "ponti.pagamento.pix-aprovacao-segundos=0")
@AutoConfigureMockMvc
@Transactional
class PagamentoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AgendamentoRepository agendamentoRepository;
    @Autowired GatewayPagamentoSimulado gateway;

    Long mentorId;
    Long startupId;
    LocalDateTime horario;

    @BeforeEach
    void preparar() throws Exception {
        Mentor m = new Mentor();
        m.setNome("Mentora Paga");
        m.setValorSessao(new BigDecimal("120.00"));
        m.setDuracaoSessaoMinutos(60);
        mentorId = mentorRepository.save(m).getId();
        Startup s = new Startup();
        s.setNome("Startup Pagante");
        startupId = startupRepository.save(s).getId();
        LocalDate terca = LocalDate.now(RelogioConfig.FUSO).with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);
        horario = terca.atTime(9, 0);
        enviar(put("/mentores/" + mentorId + "/disponibilidade"), Login.comoMentor(mentorId),
                List.of(Map.of("diaSemana", "TUESDAY", "horaInicio", "09:00", "horaFim", "12:00")))
                .andExpect(status().isOk());
    }

    ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                         org.springframework.test.web.servlet.request.RequestPostProcessor login, Object corpo) throws Exception {
        return mockMvc.perform(req.with(login).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    Map<String, Object> pedido(String metodo, String numeroCartao, String validade) {
        Map<String, Object> body = new HashMap<>();
        body.put("startupId", startupId);
        body.put("mentorId", mentorId);
        body.put("inicio", horario.toString());
        body.put("metodoPagamento", metodo);
        if (numeroCartao != null) {
            body.put("cartao", Map.of("numero", numeroCartao, "nomeImpresso", "MARIA SILVA", "validade", validade, "cvv", "123"));
        }
        return body;
    }

    Long agendarPix() throws Exception {
        String json = enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("PIX", null, null))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    @Nested
    class Pix {

        @Test
        void geraOCopiaEColaEFicaAguardando() throws Exception {
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("PIX", null, null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusPagamento").value("AGUARDANDO"))
                    .andExpect(jsonPath("$.pagamento.codigoTransacao").value(org.hamcrest.Matchers.startsWith("PNT-")))
                    .andExpect(jsonPath("$.pagamento.pixCopiaECola").value(org.hamcrest.Matchers.startsWith("000201")))
                    .andExpect(jsonPath("$.pagamento.expiraEm").exists());
        }

        @Test
        void mentorNaoVeNemConfirmaAntesDoPagamento() throws Exception {
            Long id = agendarPix();

            mockMvc.perform(get("/agendamentos").with(Login.comoMentor(mentorId)))
                    .andExpect(jsonPath("$.length()").value(0));
            enviar(post("/agendamentos/" + id + "/confirmar"), Login.comoMentor(mentorId),
                    Map.of("linkReuniao", "https://meet.google.com/abc"))
                    .andExpect(status().isConflict());
        }

        @Test
        void oBancoConfirmaEOPedidoChegaAoMentor() throws Exception {
            Long id = agendarPix();

            gateway.processarPixPendentes();

            mockMvc.perform(get("/agendamentos/" + id).with(Login.comoStartup(startupId)))
                    .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                    .andExpect(jsonPath("$.pagamento.pagoEm").exists())
                    .andExpect(jsonPath("$.pagamento.pixCopiaECola").doesNotExist());
            mockMvc.perform(get("/agendamentos").with(Login.comoMentor(mentorId)))
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void pixVencidoExpiraELiberaOHorario() throws Exception {
            Long id = agendarPix();
            Agendamento a = agendamentoRepository.findById(id).orElseThrow();
            a.setPagamentoExpiraEm(LocalDateTime.now(RelogioConfig.FUSO).minusMinutes(1));

            gateway.processarPixPendentes();

            assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
            assertThat(a.getStatusPagamento()).isEqualTo(StatusPagamento.EXPIRADO);
            // O mesmo horário pode ser agendado de novo
            agendarPix();
        }

        @Test
        void cancelarAntesDePagarEncerraACobranca() throws Exception {
            Long id = agendarPix();
            mockMvc.perform(post("/agendamentos/" + id + "/cancelar").with(Login.comoStartup(startupId)))
                    .andExpect(jsonPath("$.status").value("CANCELADO"))
                    .andExpect(jsonPath("$.statusPagamento").value("EXPIRADO"));
        }

        @Test
        void soQuemParticipaVeOAgendamento() throws Exception {
            Long id = agendarPix();
            mockMvc.perform(get("/agendamentos/" + id).with(Login.comoStartup(startupId + 999)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Cartao {

        @Test
        void aprovadoGuardaSoBandeiraEFinal() throws Exception {
            String json = enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", "4242 4242 4242 4242", "12/35"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                    .andExpect(jsonPath("$.pagamento.cartaoBandeira").value("Visa"))
                    .andExpect(jsonPath("$.pagamento.cartaoFinal").value("4242"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(json).doesNotContain("4242 4242").doesNotContain("4242424242424242").doesNotContain("\"cvv\"");
        }

        @Test
        void cartaoDeTesteRecusadoNaoCriaOAgendamento() throws Exception {
            long antes = agendamentoRepository.count();
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", "4000 0000 0000 0002", "12/35"))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.title").value("Pagamento recusado"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("recusado")));
            assertThat(agendamentoRepository.count()).isEqualTo(antes);
        }

        @Test
        void numeroInvalidoOuCartaoVencidoDa422() throws Exception {
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", "4242 4242 4242 4241", "12/35"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Número do cartão inválido. Confira os dígitos."));
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", "4242 4242 4242 4242", "01/20"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Cartão vencido. Use outro cartão."));
        }

        @Test
        void semDadosDoCartaoOuComFormatoErradoDa400() throws Exception {
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", null, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Informe os dados do cartão"));
            enviar(post("/agendamentos"), Login.comoStartup(startupId), pedido("CARTAO", "4242 4242 4242 4242", "13/35"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0].campo").value("cartao.validade"));
        }
    }
}
