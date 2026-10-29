package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Tema;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Temas: em que a startup precisa de ajuda e em que o mentor ajuda (base do match).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TemasTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;

    ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, Object corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
    }

    @Test
    void listaDeTemasEPublica() throws Exception {
        mockMvc.perform(get("/temas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Tema.values().length))
                .andExpect(jsonPath("$[0].codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$[0].nome").value("Validação de ideia"));
    }

    @Test
    void cadastroDaStartupGuardaAsNecessidades() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(AuthControllerTest.startupValida());
        body.put("necessidades", List.of("VALIDACAO", "PITCH"));
        enviar(post("/auth/cadastro/startup"), body).andExpect(status().isCreated());

        Startup s = startupRepository.findAll().stream()
                .filter(x -> "AgroTech Recife".equals(x.getNome())).findFirst().orElseThrow();
        assertThat(s.getNecessidades()).containsExactlyInAnyOrder(Tema.VALIDACAO, Tema.PITCH);
    }

    @Test
    void startupNaoPodeTerMaisDeTresNecessidades() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(AuthControllerTest.startupValida());
        body.put("necessidades", List.of("VALIDACAO", "PITCH", "VENDAS", "JURIDICO"));
        enviar(post("/auth/cadastro/startup"), body).andExpect(status().isBadRequest());
    }

    @Test
    void temaInexistenteERejeitado() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(AuthControllerTest.startupValida());
        body.put("necessidades", List.of("ASTROLOGIA"));
        enviar(post("/auth/cadastro/startup"), body).andExpect(status().isBadRequest());
    }

    @Test
    void cadastroDoMentorGuardaOsTemasEAPesquisaDevolve() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(AuthControllerTest.mentorValido());
        body.put("temas", List.of("PRODUTO", "TECNOLOGIA"));
        enviar(post("/auth/cadastro/mentor"), body).andExpect(status().isCreated());

        Mentor m = mentorRepository.findAll().stream()
                .filter(x -> "Maria Souza".equals(x.getNome())).findFirst().orElseThrow();
        mockMvc.perform(get("/mentores/" + m.getId()).with(Login.comoMentor(m.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temas.length()").value(2));
    }

    @Test
    void patchTrocaApagaOuMantemOsTemas() throws Exception {
        Startup s = new Startup();
        s.setNome("Startup X");
        s.setNecessidades(java.util.Set.of(Tema.VENDAS));
        s = startupRepository.save(s);
        var login = Login.comoStartup(s.getId());

        // trocar
        enviar(patch("/startups/" + s.getId()).with(login), Map.of("necessidades", List.of("PITCH", "FINANCAS")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.necessidades.length()").value(2));
        // mudar outro campo mantém os temas
        enviar(patch("/startups/" + s.getId()).with(login), Map.of("cidade", "Recife"))
                .andExpect(jsonPath("$.necessidades.length()").value(2));
        // lista vazia apaga
        enviar(patch("/startups/" + s.getId()).with(login), Map.of("necessidades", List.of()))
                .andExpect(jsonPath("$.necessidades.length()").value(0));

        Mentor m = new Mentor();
        m.setNome("Mentor X");
        m.setValorSessao(new BigDecimal("0"));
        m.setDuracaoSessaoMinutos(30);
        m = mentorRepository.save(m);
        enviar(patch("/mentores/" + m.getId()).with(Login.comoMentor(m.getId())), Map.of("temas", List.of("JURIDICO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.temas[0]").value("JURIDICO"));
    }
}
