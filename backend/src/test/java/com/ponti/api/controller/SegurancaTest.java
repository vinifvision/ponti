package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.DadosIniciais;
import com.ponti.api.pagamento.GatewayPagamentoSimulado;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Segurança de ponta a ponta, com tokens JWT de verdade (login pela API):
// rotas exigem login (401), cada um só mexe no que é seu (403) e os erros seguem o mesmo formato.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SegurancaTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AgendamentoRepository agendamentoRepository;
    @Autowired GatewayPagamentoSimulado gateway;

    String tokenPetcare;
    String tokenJuliana;
    String tokenRoberto;
    Long petcareId;
    Long saudeAgoraId;
    Long julianaId;
    Long robertoId;

    @BeforeEach
    void entrar() throws Exception {
        tokenPetcare = token("petcare@exemplo.com", DadosIniciais.SENHA_EXEMPLO);
        tokenJuliana = token("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO);
        tokenRoberto = token("roberto@exemplo.com", DadosIniciais.SENHA_EXEMPLO);
        petcareId = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow().getStartupId();
        saudeAgoraId = usuarioRepository.findByEmail("saudeagora@exemplo.com").orElseThrow().getStartupId();
        julianaId = usuarioRepository.findByEmail("juliana@exemplo.com").orElseThrow().getMentorId();
        robertoId = usuarioRepository.findByEmail("roberto@exemplo.com").orElseThrow().getMentorId();
    }

    // ---------------------------------------------------------------- apoio

    String token(String email, String senha) throws Exception {
        String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("token").asText();
    }

    ResultActions como(String token, MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header(HttpHeaders.AUTHORIZATION, token));
    }

    ResultActions como(String token, MockHttpServletRequestBuilder req, Object corpo) throws Exception {
        return mockMvc.perform(req.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    // Agenda o primeiro horário livre da Juliana em nome da PetCare e devolve o id
    Long agendarComJuliana() throws Exception {
        String hoje = java.time.LocalDate.now().toString();
        String ate = java.time.LocalDate.now().plusDays(30).toString();
        JsonNode horarios = objectMapper.readTree(como(tokenPetcare,
                get("/mentores/" + julianaId + "/horarios").param("de", hoje).param("ate", ate))
                .andReturn().getResponse().getContentAsString());
        String json = como(tokenPetcare, post("/agendamentos"), Map.of(
                "startupId", petcareId, "mentorId", julianaId,
                "inicio", horarios.get(0).asText(), "metodoPagamento", "PIX"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();
        gateway.confirmarPix(agendamentoRepository.findById(id).orElseThrow()); // "banco" confirma o Pix
        return id;
    }

    // ---------------------------------------------------------------- testes

    @Nested
    class Login {

        @Test
        void semTokenDa401NoFormatoPadraoDeErro() throws Exception {
            mockMvc.perform(get("/mentores"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Login necessário"))
                    .andExpect(jsonPath("$.message").value("Faça login para continuar"))
                    .andExpect(jsonPath("$.instance").value("/mentores"));
        }

        @Test
        void tokenInvalidoDa401() throws Exception {
            como("Bearer token.falso.invalido", get("/mentores")).andExpect(status().isUnauthorized());
        }

        @Test
        void comTokenValidoAcessa() throws Exception {
            como(tokenPetcare, get("/mentores")).andExpect(status().isOk());
        }

        @Test
        void loginECadastroSaoPublicos() throws Exception {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x@x.com\",\"senha\":\"errada123\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        }

        @Test
        void navegadorConsultaCorsSemToken() throws Exception {
            mockMvc.perform(options("/mentores/1/disponibilidade")
                            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        }

        @Test
        void corsLiberaOPatchDoMeuPerfil() throws Exception {
            mockMvc.perform(options("/mentores/1")
                            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("PATCH")));
        }
    }

    @Nested
    class Dono {

        @Test
        void startupNaoAgendaEmNomeDeOutra() throws Exception {
            como(tokenPetcare, post("/agendamentos"), Map.of(
                    "startupId", saudeAgoraId, "mentorId", julianaId,
                    "inicio", "2099-01-01T09:00:00", "metodoPagamento", "PIX"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso negado"));
        }

        @Test
        void soOMentorDoPedidoConfirma() throws Exception {
            Long id = agendarComJuliana();
            Map<String, String> link = Map.of("linkReuniao", "https://meet.google.com/abc-defg-hij");

            como(tokenPetcare, post("/agendamentos/" + id + "/confirmar"), link).andExpect(status().isForbidden());
            como(tokenRoberto, post("/agendamentos/" + id + "/confirmar"), link).andExpect(status().isForbidden());
            como(tokenJuliana, post("/agendamentos/" + id + "/confirmar"), link)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMADO"));
        }

        @Test
        void soQuemParticipaCancela() throws Exception {
            Long id = agendarComJuliana();

            como(tokenRoberto, post("/agendamentos/" + id + "/cancelar")).andExpect(status().isForbidden());
            como(tokenPetcare, post("/agendamentos/" + id + "/cancelar")).andExpect(status().isOk());
        }

        @Test
        void mentorNaoMudaAAgendaDeOutro() throws Exception {
            como(tokenJuliana, put("/mentores/" + robertoId + "/disponibilidade"), List.of())
                    .andExpect(status().isForbidden());
        }

        @Test
        void ninguemVeAsMentoriasDeOutroPerfil() throws Exception {
            como(tokenPetcare, get("/agendamentos").param("startupId", saudeAgoraId.toString()))
                    .andExpect(status().isForbidden());
            como(tokenPetcare, get("/agendamentos").param("mentorId", julianaId.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void semParametrosCadaUmVeAsSuas() throws Exception {
            agendarComJuliana();

            como(tokenJuliana, get("/agendamentos")).andExpect(jsonPath("$", hasSize(1)));
            como(tokenRoberto, get("/agendamentos")).andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void startupNaoEditaPerfilDeOutra() throws Exception {
            Map<String, Object> corpo = Map.of("nome", "Tomada");
            como(tokenPetcare, put("/startups/" + saudeAgoraId), corpo).andExpect(status().isForbidden());
            assertThat(startupRepository.findById(saudeAgoraId).orElseThrow().getNome()).isEqualTo("SaúdeAgora");
        }

        @Test
        void mentorNaoSugereMentoriaEmNomeDeOutro() throws Exception {
            como(tokenJuliana, post("/convites"), Map.of(
                    "startupId", petcareId, "mentorId", robertoId, "enviadoPor", "MENTOR"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void soAStartupRespondeASugestaoQueRecebeu() throws Exception {
            String json = como(tokenJuliana, post("/convites"), Map.of(
                    "startupId", petcareId, "mentorId", julianaId, "enviadoPor", "MENTOR"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(json).get("id").asLong();

            como(tokenJuliana, post("/convites/" + id + "/recusar")).andExpect(status().isForbidden());
            como(tokenPetcare, post("/convites/" + id + "/recusar")).andExpect(status().isOk());
        }
    }

    @Nested
    class Conta {

        @Test
        void perfisSoSaoCriadosPeloCadastro() throws Exception {
            como(tokenPetcare, post("/mentores"), Map.of("nome", "Fantasma"))
                    .andExpect(status().isMethodNotAllowed());
            como(tokenPetcare, post("/startups"), Map.of("nome", "Fantasma"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void excluirAContaApagaOLoginEOsAgendamentos() throws Exception {
            Long agendamento = agendarComJuliana();

            como(tokenPetcare, delete("/mentores/" + julianaId)).andExpect(status().isForbidden());
            como(tokenJuliana, delete("/mentores/" + julianaId)).andExpect(status().isNoContent());

            assertThat(mentorRepository.existsById(julianaId)).isFalse();
            assertThat(usuarioRepository.findByEmail("juliana@exemplo.com")).isEmpty();
            assertThat(agendamentoRepository.existsById(agendamento)).isFalse();
            // O token antigo deixa de valer, porque o usuário não existe mais
            como(tokenJuliana, get("/mentores")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Erros {

        @Test
        void validacaoListaOsCamposEmPortugues() throws Exception {
            Map<String, Object> corpo = new HashMap<>(AuthControllerTest.mentorValido());
            corpo.remove("valorSessao");
            corpo.put("email", "nao-e-email");

            mockMvc.perform(post("/auth/cadastro/mentor").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.campos", hasSize(2)))
                    .andExpect(jsonPath("$.campos[0].campo").value("email"))
                    .andExpect(jsonPath("$.campos[1].campo").value("valorSessao"))
                    .andExpect(jsonPath("$.campos[1].mensagem").value("não deve ser nulo"));
        }

        @Test
        void jsonMalFormadoDa400ComMensagemClara() throws Exception {
            como(tokenPetcare, post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content("{ nada"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("O corpo da requisição tem um formato inválido. Confira os tipos dos campos."));
        }

        @Test
        void naoEncontradoUsaOMesmoFormato() throws Exception {
            como(tokenPetcare, get("/mentores/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Não encontrado"))
                    .andExpect(jsonPath("$.message").value("Mentor não encontrado(a)"));
        }
    }
}
