package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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

// Perfil completo do mentor: preço, duração, vídeo, contatos e certificações.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilMentorTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired CertificacaoRepository certificacaoRepository;

    Map<String, Object> mentorCompleto() {
        Map<String, Object> body = new HashMap<>(AuthControllerTest.mentorValido());
        body.put("ecossistemas", "Porto Digital, Health-techs");
        body.put("fotoUrl", "https://exemplo.com/foto.jpg");
        body.put("videoUrl", "https://www.youtube.com/watch?v=abc123");
        body.put("linkedinUrl", "https://www.linkedin.com/in/maria");
        body.put("whatsapp", "+55 (81) 99999-0000");
        body.put("certificacoes", List.of(
                Map.of("titulo", "Mentoria programa Sebrae", "instituicao", "Sebrae",
                        "anoInicio", 2024, "anoFim", 2025, "descricao", "Mentoria de produto"),
                Map.of("titulo", "Mentoria Porto Digital", "anoInicio", 2026)));
        return body;
    }

    ResultActions enviarJson(MockHttpServletRequestBuilder req,
                             Object body) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    Mentor cadastrar(Map<String, Object> body) throws Exception {
        enviarJson(post("/auth/cadastro/mentor"), body).andExpect(status().isCreated());
        Usuario usuario = usuarioRepository.findByEmail((String) body.get("email")).orElseThrow();
        return mentorRepository.findById(usuario.getMentorId()).orElseThrow();
    }

    @Nested
    class Cadastro {

        @Test
        void gravaPrecoDuracaoVideoEContatos() throws Exception {
            Mentor mentor = cadastrar(mentorCompleto());

            assertThat(mentor.getValorSessao()).isEqualByComparingTo(new BigDecimal("150"));
            assertThat(mentor.getDuracaoSessaoMinutos()).isEqualTo(60);
            assertThat(mentor.getEcossistemas()).isEqualTo("Porto Digital, Health-techs");
            assertThat(mentor.getFotoUrl()).isEqualTo("https://exemplo.com/foto.jpg");
            assertThat(mentor.getVideoUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
            assertThat(mentor.getLinkedinUrl()).isEqualTo("https://www.linkedin.com/in/maria");
            assertThat(mentor.getWhatsapp()).isEqualTo("+55 (81) 99999-0000");
        }

        @Test
        void aceitaMentoriaGratuita() throws Exception {
            Map<String, Object> body = mentorCompleto();
            body.put("valorSessao", 0);

            Mentor mentor = cadastrar(body);

            assertThat(mentor.getValorSessao()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void gravaAsCertificacoesDoCadastro() throws Exception {
            Mentor mentor = cadastrar(mentorCompleto());

            assertThat(certificacaoRepository.findByMentorIdOrderByAnoInicioDescIdDesc(mentor.getId()))
                    .extracting("titulo")
                    .containsExactly("Mentoria Porto Digital", "Mentoria programa Sebrae");
        }

        @Test
        void recusaCertificacaoSemTituloENaoGravaNada() throws Exception {
            long mentoresAntes = mentorRepository.count();
            Map<String, Object> body = mentorCompleto();
            body.put("certificacoes", List.of(Map.of("instituicao", "Sebrae")));

            enviarJson(post("/auth/cadastro/mentor"), body).andExpect(status().isBadRequest());

            assertThat(mentorRepository.count()).isEqualTo(mentoresAntes);
        }

        @Test
        void recusaCertificacaoQueTerminaAntesDeComecar() throws Exception {
            Map<String, Object> body = mentorCompleto();
            body.put("certificacoes", List.of(Map.of("titulo", "Curso", "anoInicio", 2025, "anoFim", 2020)));

            enviarJson(post("/auth/cadastro/mentor"), body).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Privacidade {

        @Test
        void listagemDeMentoresMostraPrecoMasNuncaOWhatsapp() throws Exception {
            Mentor mentor = cadastrar(mentorCompleto());

            mockMvc.perform(get("/mentores/" + mentor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valorSessao").value(150))
                    .andExpect(jsonPath("$.duracaoSessaoMinutos").value(60))
                    .andExpect(jsonPath("$.linkedinUrl").value("https://www.linkedin.com/in/maria"))
                    .andExpect(jsonPath("$", not(hasKey("whatsapp"))));
        }

        @Test
        void editarPerfilSemEnviarWhatsappNaoApagaONumero() throws Exception {
            Mentor mentor = cadastrar(mentorCompleto());

            enviarJson(put("/mentores/" + mentor.getId()),
                    Map.of("nome", "Maria S.", "valorSessao", 200, "duracaoSessaoMinutos", 45))
                    .andExpect(status().isOk());

            Mentor atualizado = mentorRepository.findById(mentor.getId()).orElseThrow();
            assertThat(atualizado.getNome()).isEqualTo("Maria S.");
            assertThat(atualizado.getWhatsapp()).isEqualTo("+55 (81) 99999-0000");
        }

        @Test
        void startupSoRecebeOWhatsappDepoisQueOConviteEAceito() throws Exception {
            Mentor mentor = cadastrar(mentorCompleto());
            Long startupId = 1L; // PetCare Digital, criada em DadosIniciais

            String json = enviarJson(post("/convites"), Map.of("startupId", startupId, "mentorId", mentor.getId()))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long conviteId = objectMapper.readTree(json).get("id").asLong();

            assertThat(mentorDoConvite(startupId, conviteId).get("whatsapp").isNull()).isTrue();

            mockMvc.perform(post("/convites/" + conviteId + "/aceitar")).andExpect(status().isOk());

            JsonNode depois = mentorDoConvite(startupId, conviteId);
            assertThat(depois.get("whatsapp").asText()).isEqualTo("+55 (81) 99999-0000");
            assertThat(depois.get("valorSessao").decimalValue()).isEqualByComparingTo("150");
        }

        // Procura o convite na lista de enviados da startup e devolve os dados do mentor
        JsonNode mentorDoConvite(Long startupId, Long conviteId) throws Exception {
            String json = mockMvc.perform(get("/convites/detalhados").param("startupId", startupId.toString()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            for (JsonNode convite : objectMapper.readTree(json)) {
                if (convite.get("id").asLong() == conviteId) {
                    return convite.get("mentor");
                }
            }
            throw new AssertionError("Convite " + conviteId + " não apareceu na lista da startup");
    }
    }

    @Nested
    class Certificacoes {

        @Test
        void adicionaListaERemoveCertificacao() throws Exception {
            Mentor mentor = cadastrar(AuthControllerTest.mentorValido());
            String base = "/mentores/" + mentor.getId() + "/certificacoes";

            String json = enviarJson(post(base), Map.of("titulo", "Certificação Scrum", "anoInicio", 2023))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mentorId").value(mentor.getId()))
                    .andReturn().getResponse().getContentAsString();
            long certificacaoId = objectMapper.readTree(json).get("id").asLong();

            mockMvc.perform(get(base))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].titulo").value("Certificação Scrum"));

            mockMvc.perform(delete(base + "/" + certificacaoId)).andExpect(status().isNoContent());
            mockMvc.perform(get(base)).andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void naoRemoveCertificacaoDeOutroMentor() throws Exception {
            Mentor dono = cadastrar(AuthControllerTest.mentorValido());
            Map<String, Object> outroBody = new HashMap<>(AuthControllerTest.mentorValido());
            outroBody.put("email", "outro@email.com");
            Mentor outro = cadastrar(outroBody);

            String json = enviarJson(post("/mentores/" + dono.getId() + "/certificacoes"), Map.of("titulo", "Curso"))
                    .andReturn().getResponse().getContentAsString();
            long certificacaoId = objectMapper.readTree(json).get("id").asLong();

            mockMvc.perform(delete("/mentores/" + outro.getId() + "/certificacoes/" + certificacaoId))
                    .andExpect(status().isNotFound());
            assertThat(certificacaoRepository.existsById(certificacaoId)).isTrue();
        }

        @Test
        void mentorInexistenteDa404() throws Exception {
            mockMvc.perform(get("/mentores/999999/certificacoes")).andExpect(status().isNotFound());
        }
    }
}
