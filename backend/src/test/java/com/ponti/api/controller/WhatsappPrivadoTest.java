package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
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

// O WhatsApp é um dado de contato privado: fica gravado, não aparece nos perfis e só é mostrado
// à outra parte de uma mentoria CONFIRMADA, e apenas se o dono autorizou compartilhar.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WhatsappPrivadoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired AgendamentoRepository agendamentoRepository;

    // ---------------------------------------------------------------- apoio

    ResultActions enviarJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private Startup cadastrarStartup(boolean compartilhar) throws Exception {
        Map<String, Object> body = AuthControllerTest.startupValida();
        body.put("whatsapp", "81988887777");
        body.put("compartilharWhatsapp", compartilhar);
        enviarJson(post("/auth/cadastro/startup"), body).andExpect(status().isCreated());
        Usuario usuario = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow();
        return startupRepository.findById(usuario.getStartupId()).orElseThrow();
    }

    private Mentor cadastrarMentor(boolean compartilhar) throws Exception {
        Map<String, Object> body = AuthControllerTest.mentorValido();
        body.put("compartilharWhatsapp", compartilhar);
        enviarJson(post("/auth/cadastro/mentor"), body).andExpect(status().isCreated());
        Usuario usuario = usuarioRepository.findByEmail("maria@email.com").orElseThrow();
        return mentorRepository.findById(usuario.getMentorId()).orElseThrow();
    }

    private Long agendamento(Startup s, Mentor m, StatusAgendamento statusAgendamento) {
        Agendamento a = new Agendamento();
        a.setStartupId(s.getId());
        a.setMentorId(m.getId());
        a.setInicio(LocalDateTime.now().plusDays(3));
        a.setFim(LocalDateTime.now().plusDays(3).plusHours(1));
        a.setValor(BigDecimal.ZERO);
        a.setStatus(statusAgendamento);
        a.setStatusPagamento(StatusPagamento.GRATUITA);
        return agendamentoRepository.save(a).getId();
    }

    // ---------------------------------------------------------------- testes

    @Nested
    class Perfis {

        @Test
        void perfilDaStartupNaoMostraWhatsapp() throws Exception {
            Startup startup = cadastrarStartup(true);

            mockMvc.perform(get("/startups/" + startup.getId()).with(Login.comoMentor(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(hasKey("whatsapp"))))
                    .andExpect(jsonPath("$", not(hasKey("compartilharWhatsapp"))));
            mockMvc.perform(get("/startups").with(Login.comoMentor(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]", not(hasKey("whatsapp"))));
        }

        @Test
        void putCompletoNaoApagaWhatsappNemAutorizacao() throws Exception {
            Startup startup = cadastrarStartup(true);

            // O PUT manda o perfil inteiro, mas o JSON nunca tem o WhatsApp
            enviarJson(put("/startups/" + startup.getId()).with(Login.comoStartup(startup.getId())),
                    Map.of("nome", "AgroTech Recife", "estagioMaturidade", 3))
                    .andExpect(status().isOk());

            Startup salva = startupRepository.findById(startup.getId()).orElseThrow();
            assertThat(salva.getWhatsapp()).isEqualTo("5581988887777");
            assertThat(salva.isCompartilharWhatsapp()).isTrue();
        }
    }

    @Nested
    class ContatoDoDono {

        @Test
        void donoVeOProprioContato() throws Exception {
            Mentor mentor = cadastrarMentor(true);

            mockMvc.perform(get("/mentores/" + mentor.getId() + "/contato").with(Login.comoMentor(mentor.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.whatsapp").value("5581999998888"))
                    .andExpect(jsonPath("$.compartilharWhatsapp").value(true));
        }

        @Test
        void outroUsuarioNaoVeOContato() throws Exception {
            Mentor mentor = cadastrarMentor(true);
            Startup startup = cadastrarStartup(true);

            mockMvc.perform(get("/mentores/" + mentor.getId() + "/contato").with(Login.comoStartup(startup.getId())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/startups/" + startup.getId() + "/contato").with(Login.comoMentor(mentor.getId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void autorizacaoComecaDesligada() throws Exception {
            Map<String, Object> body = AuthControllerTest.startupValida();
            body.put("whatsapp", "81988887777");
            enviarJson(post("/auth/cadastro/startup"), body).andExpect(status().isCreated());
            Usuario usuario = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow();

            mockMvc.perform(get("/startups/" + usuario.getStartupId() + "/contato").with(Login.comoStartup(usuario.getStartupId())))
                    .andExpect(jsonPath("$.compartilharWhatsapp").value(false));
        }

        @Test
        void patchTrocaNumeroEAutorizacao() throws Exception {
            Mentor mentor = cadastrarMentor(false);

            enviarJson(patch("/mentores/" + mentor.getId()).with(Login.comoMentor(mentor.getId())),
                    Map.of("whatsapp", "(11) 98888-7777", "compartilharWhatsapp", true))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(hasKey("whatsapp"))));

            Mentor salvo = mentorRepository.findById(mentor.getId()).orElseThrow();
            assertThat(salvo.getWhatsapp()).isEqualTo("5511988887777");
            assertThat(salvo.isCompartilharWhatsapp()).isTrue();
        }

        @Test
        void mentorNaoPodeApagarOWhatsapp() throws Exception {
            Mentor mentor = cadastrarMentor(false);

            enviarJson(patch("/mentores/" + mentor.getId()).with(Login.comoMentor(mentor.getId())), Map.of("whatsapp", ""))
                    .andExpect(status().isBadRequest());
            assertThat(mentorRepository.findById(mentor.getId()).orElseThrow().getWhatsapp()).isEqualTo("5581999998888");
        }

        @Test
        void startupPodeApagarOWhatsapp() throws Exception {
            Startup startup = cadastrarStartup(true);

            enviarJson(patch("/startups/" + startup.getId()).with(Login.comoStartup(startup.getId())), Map.of("whatsapp", ""))
                    .andExpect(status().isOk());
            assertThat(startupRepository.findById(startup.getId()).orElseThrow().getWhatsapp()).isNull();
        }

        @Test
        void patchRecusaNumeroInvalido() throws Exception {
            Startup startup = cadastrarStartup(true);

            enviarJson(patch("/startups/" + startup.getId()).with(Login.comoStartup(startup.getId())), Map.of("whatsapp", "123"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class NasMentorias {

        @Test
        void confirmadaEAutorizadoMostraOsDoisLados() throws Exception {
            Mentor mentor = cadastrarMentor(true);
            Startup startup = cadastrarStartup(true);
            Long id = agendamento(startup, mentor, StatusAgendamento.CONFIRMADO);

            mockMvc.perform(get("/agendamentos/" + id).with(Login.comoStartup(startup.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mentor.whatsapp").value("5581999998888"));
            mockMvc.perform(get("/agendamentos").with(Login.comoMentor(mentor.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].startup.whatsapp").value("5581988887777"));
        }

        @Test
        void semAutorizacaoNaoMostra() throws Exception {
            Mentor mentor = cadastrarMentor(false);
            Startup startup = cadastrarStartup(true);
            Long id = agendamento(startup, mentor, StatusAgendamento.CONFIRMADO);

            mockMvc.perform(get("/agendamentos/" + id).with(Login.comoStartup(startup.getId())))
                    .andExpect(jsonPath("$.mentor.whatsapp").value(nullValue()))
                    .andExpect(jsonPath("$.startup.whatsapp").value("5581988887777"));
        }

        @Test
        void naoConfirmadaNaoMostra() throws Exception {
            Mentor mentor = cadastrarMentor(true);
            Startup startup = cadastrarStartup(true);

            for (StatusAgendamento s : new StatusAgendamento[] {
                    StatusAgendamento.PENDENTE, StatusAgendamento.RECUSADO, StatusAgendamento.CANCELADO}) {
                Long id = agendamento(startup, mentor, s);
                mockMvc.perform(get("/agendamentos/" + id).with(Login.comoStartup(startup.getId())))
                        .andExpect(jsonPath("$.mentor.whatsapp").value(nullValue()))
                        .andExpect(jsonPath("$.startup.whatsapp").value(nullValue()));
            }
        }
    }
}
