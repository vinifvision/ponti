package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// PATCH (edição parcial) do perfil: só os campos enviados mudam, o resto fica como estava.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilParcialTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;

    ResultActions patchJson(String url, org.springframework.test.web.servlet.request.RequestPostProcessor login, Object corpo) throws Exception {
        return mockMvc.perform(patch(url).with(login).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    Mentor mentor() {
        Mentor m = new Mentor();
        m.setNome("Mentora Original");
        m.setBio("Bio antiga");
        m.setAreasAtuacao("Produto");
        m.setValorSessao(new BigDecimal("100.00"));
        m.setDuracaoSessaoMinutos(60);
        return mentorRepository.save(m);
    }

    @Test
    void mentorMudaSoOQueEnviou() throws Exception {
        Mentor m = mentor();

        patchJson("/mentores/" + m.getId(), Login.comoMentor(m.getId()), Map.of("bio", "Bio nova", "valorSessao", 150))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Bio nova"))
                .andExpect(jsonPath("$.valorSessao").value(150))
                .andExpect(jsonPath("$.nome").value("Mentora Original"))
                .andExpect(jsonPath("$.areasAtuacao").value("Produto"))
                .andExpect(jsonPath("$.duracaoSessaoMinutos").value(60));
    }

    @Test
    void textoVazioApagaOCampo() throws Exception {
        Mentor m = mentor();
        patchJson("/mentores/" + m.getId(), Login.comoMentor(m.getId()), Map.of("bio", ""))
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void naoEditaPerfilDeOutroEValidaOsCampos() throws Exception {
        Mentor m = mentor();
        patchJson("/mentores/" + m.getId(), Login.comoMentor(m.getId() + 999), Map.of("bio", "x"))
                .andExpect(status().isForbidden());
        patchJson("/mentores/" + m.getId(), Login.comoMentor(m.getId()), Map.of("duracaoSessaoMinutos", 5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("duracaoSessaoMinutos"));
    }

    @Test
    void startupMudaSoOQueEnviou() throws Exception {
        Startup s = new Startup();
        s.setNome("Startup Original");
        s.setSegmento("Agritech");
        s.setEstagioMaturidade(2);
        s = startupRepository.save(s);

        patchJson("/startups/" + s.getId(), Login.comoStartup(s.getId()), Map.of("estagioMaturidade", 4, "cidade", "Recife"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estagioMaturidade").value(4))
                .andExpect(jsonPath("$.cidade").value("Recife"))
                .andExpect(jsonPath("$.segmento").value("Agritech"));
        assertThat(startupRepository.findById(s.getId()).orElseThrow().getNome()).isEqualTo("Startup Original");
    }
}
