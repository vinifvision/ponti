package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.support.Login;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// O WhatsApp é um dado de contato privado: fica gravado, mas não aparece nas respostas da API.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WhatsappPrivadoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired StartupRepository startupRepository;

    private Startup cadastrarStartupComWhatsapp() throws Exception {
        Map<String, Object> body = AuthControllerTest.startupValida();
        body.put("whatsapp", "81988887777");
        mockMvc.perform(post("/auth/cadastro/startup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
        Usuario usuario = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow();
        return startupRepository.findById(usuario.getStartupId()).orElseThrow();
    }

    @Test
    void perfilDaStartupNaoMostraWhatsapp() throws Exception {
        Startup startup = cadastrarStartupComWhatsapp();

        mockMvc.perform(get("/startups/" + startup.getId()).with(Login.comoMentor(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("whatsapp"))));
        mockMvc.perform(get("/startups").with(Login.comoMentor(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", not(hasKey("whatsapp"))));
    }

    @Test
    void putCompletoNaoApagaWhatsapp() throws Exception {
        Startup startup = cadastrarStartupComWhatsapp();

        // O PUT manda o perfil inteiro, mas o JSON nunca tem o WhatsApp
        Map<String, Object> perfil = Map.of("nome", "AgroTech Recife", "estagioMaturidade", 3);
        mockMvc.perform(put("/startups/" + startup.getId())
                        .with(Login.comoStartup(startup.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isOk());

        assertThat(startupRepository.findById(startup.getId()).orElseThrow().getWhatsapp())
                .isEqualTo("5581988887777");
    }
}
