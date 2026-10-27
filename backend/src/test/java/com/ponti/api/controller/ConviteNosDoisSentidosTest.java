package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import java.util.HashMap;
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

// Convite nos dois sentidos (startup -> mentor e mentor -> startup) e perfil público da startup.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConviteNosDoisSentidosTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired ConviteRepository conviteRepository;
    @Autowired CertificacaoRepository certificacaoRepository;

    Long startupId;
    Long mentorId;

    // Cada teste usa um par novo, para não depender dos dados de exemplo
    @BeforeEach
    void criarPar() {
        Startup s = new Startup();
        s.setNome("Startup do Teste");
        startupId = startupRepository.save(s).getId();
        Mentor m = new Mentor();
        m.setNome("Mentor do Teste");
        mentorId = mentorRepository.save(m).getId();
    }

    ResultActions enviarJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    Long criarConvite(Map<String, Object> body) throws Exception {
        String json = enviarJson(post("/convites"), body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    JsonNode detalhado(String filtro, Long valor, Long conviteId) throws Exception {
        String json = mockMvc.perform(get("/convites/detalhados").param(filtro, valor.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode convite : objectMapper.readTree(json)) {
            if (convite.get("id").asLong() == conviteId) {
                return convite;
            }
        }
        throw new AssertionError("Convite " + conviteId + " não apareceu na lista");
    }

    @Nested
    class Sentido {

        @Test
        void semInformarQuemEnviouValeStartup() throws Exception {
            Long id = criarConvite(Map.of("startupId", startupId, "mentorId", mentorId));

            assertThat(conviteRepository.findById(id).orElseThrow().getEnviadoPor())
                    .isEqualTo(TipoUsuario.STARTUP);
        }

        @Test
        void mentorConvidaStartupEElaApareceNosDoisLados() throws Exception {
            Long id = criarConvite(Map.of("startupId", startupId, "mentorId", mentorId, "enviadoPor", "MENTOR"));

            JsonNode visaoDaStartup = detalhado("startupId", startupId, id);
            assertThat(visaoDaStartup.get("enviadoPor").asText()).isEqualTo("MENTOR");
            assertThat(visaoDaStartup.get("status").asText()).isEqualTo("PENDENTE");
            assertThat(visaoDaStartup.get("mentor").get("nome").asText()).isEqualTo("Mentor do Teste");

            JsonNode visaoDoMentor = detalhado("mentorId", mentorId, id);
            assertThat(visaoDoMentor.get("startup").get("nome").asText()).isEqualTo("Startup do Teste");
        }

        @Test
        void startupAceitaConviteDoMentor() throws Exception {
            Long id = criarConvite(Map.of("startupId", startupId, "mentorId", mentorId, "enviadoPor", "MENTOR"));

            mockMvc.perform(post("/convites/" + id + "/aceitar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACEITO"))
                    .andExpect(jsonPath("$.enviadoPor").value("MENTOR"));
        }

        @Test
        void soExisteUmConvitePorParSejaQualForOSentido() throws Exception {
            criarConvite(Map.of("startupId", startupId, "mentorId", mentorId, "enviadoPor", "MENTOR"));

            enviarJson(post("/convites"), Map.of("startupId", startupId, "mentorId", mentorId, "enviadoPor", "STARTUP"))
                    .andExpect(status().isConflict())
                    .andExpect(result -> assertThat(result.getResolvedException().getMessage())
                            .contains("Já existe um convite entre essa startup e esse mentor"));
        }

        @Test
        void recusaQuemEnviouInvalido() throws Exception {
            enviarJson(post("/convites"), Map.of("startupId", startupId, "mentorId", mentorId, "enviadoPor", "INVESTIDOR"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PerfilDaStartup {

        @Test
        void cadastroGuardaOsCamposOpcionaisDoPerfil() throws Exception {
            Map<String, Object> body = new HashMap<>(AuthControllerTest.startupValida());
            body.put("cidade", "Recife");
            body.put("estado", "PE");
            body.put("videoPitchUrl", "https://www.youtube.com/watch?v=abcdefghijk");
            body.put("fotoUrl", "https://exemplo.com/logo.png");
            body.put("site", "https://agrotech.com");

            String json = enviarJson(post("/auth/cadastro/startup"), body)
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long id = objectMapper.readTree(json).get("startupId").asLong();

            Startup startup = startupRepository.findById(id).orElseThrow();
            assertThat(startup.getCidade()).isEqualTo("Recife");
            assertThat(startup.getEstado()).isEqualTo("PE");
            assertThat(startup.getVideoPitchUrl()).isEqualTo("https://www.youtube.com/watch?v=abcdefghijk");
            assertThat(startup.getFotoUrl()).isEqualTo("https://exemplo.com/logo.png");
            assertThat(startup.getSite()).isEqualTo("https://agrotech.com");
        }

        @Test
        void cadastroContinuaFuncionandoSemOsCamposOpcionais() throws Exception {
            enviarJson(post("/auth/cadastro/startup"), AuthControllerTest.startupValida())
                    .andExpect(status().isCreated());
        }

        @Test
        void recusaEstadoComMaisDeDuasLetras() throws Exception {
            Map<String, Object> body = new HashMap<>(AuthControllerTest.startupValida());
            body.put("estado", "Pernambuco");

            enviarJson(post("/auth/cadastro/startup"), body).andExpect(status().isBadRequest());
        }

        @Test
        void adicionaListaERemoveCertificacaoDaStartup() throws Exception {
            String base = "/startups/" + startupId + "/certificacoes";

            String json = enviarJson(post(base), Map.of("titulo", "Programa de aceleração", "anoInicio", 2026))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.startupId").value(startupId))
                    .andReturn().getResponse().getContentAsString();
            long certificacaoId = objectMapper.readTree(json).get("id").asLong();

            mockMvc.perform(get(base))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].titulo").value("Programa de aceleração"));

            mockMvc.perform(delete(base + "/" + certificacaoId)).andExpect(status().isNoContent());
            assertThat(certificacaoRepository.existsById(certificacaoId)).isFalse();
        }

        @Test
        void naoRemoveCertificacaoDeOutraStartup() throws Exception {
            String json = enviarJson(post("/startups/" + startupId + "/certificacoes"), Map.of("titulo", "Prêmio"))
                    .andReturn().getResponse().getContentAsString();
            long certificacaoId = objectMapper.readTree(json).get("id").asLong();

            Startup outra = new Startup();
            outra.setNome("Outra");
            Long outraId = startupRepository.save(outra).getId();

            mockMvc.perform(delete("/startups/" + outraId + "/certificacoes/" + certificacaoId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void startupInexistenteDa404() throws Exception {
            mockMvc.perform(get("/startups/999999/certificacoes")).andExpect(status().isNotFound());
        }
    }
}
