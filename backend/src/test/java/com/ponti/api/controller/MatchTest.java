package com.ponti.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Tema;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Match pela API: cada lado vê a lista do outro, ordenada por compatibilidade.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchTest {

    @Autowired MockMvc mockMvc;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;

    Mentor mentor(String nome, String setores, Tema... temas) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setEcossistemas(setores);
        m.setTemas(new LinkedHashSet<>(List.of(temas)));
        m.setValorSessao(BigDecimal.ZERO);
        m.setDuracaoSessaoMinutos(30);
        return mentorRepository.save(m);
    }

    Startup startup(String nome, String segmento, Tema... necessidades) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setSegmento(segmento);
        s.setNecessidades(new LinkedHashSet<>(List.of(necessidades)));
        return startupRepository.save(s);
    }

    @Test
    void startupVeMentoresDoMaisAoMenosCompativel() throws Exception {
        Startup s = startup("FinX", "Fintech", Tema.JURIDICO, Tema.GESTAO);
        mentor("Mentor Perfeito", "Fintechs", Tema.JURIDICO, Tema.GESTAO);
        mentor("Mentor Parcial", "Todas as áreas", Tema.JURIDICO);
        mentor("Mentor Fora", "Saúde", Tema.VENDAS);

        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(s.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mentor.nome").value("Mentor Perfeito"))
                .andExpect(jsonPath("$[0].compatibilidade").value(100))
                .andExpect(jsonPath("$[0].motivos[0]").value(org.hamcrest.Matchers.startsWith("Ajuda com")))
                .andExpect(jsonPath("$[1].mentor.nome").value("Mentor Parcial"))
                .andExpect(jsonPath("$[1].compatibilidade").value(50)) // 37,5 dos temas + 12,5 do setor, arredondado
                .andExpect(jsonPath("$[?(@.mentor.nome=='Mentor Fora')].compatibilidade").value(0));
    }

    @Test
    void mentorVeStartupsDoMaisAoMenosCompativel() throws Exception {
        Mentor m = mentor("Dra. Lei", "Fintechs", Tema.JURIDICO, Tema.GESTAO);
        startup("Combina", "Fintech", Tema.JURIDICO, Tema.GESTAO);
        startup("Nao Combina", "Agritech", Tema.TECNOLOGIA);

        mockMvc.perform(get("/startups/recomendadas").with(Login.comoMentor(m.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startup.nome").value("Combina"))
                .andExpect(jsonPath("$[0].compatibilidade").value(100))
                .andExpect(jsonPath("$[0].motivos[0]").value(org.hamcrest.Matchers.startsWith("Precisa de ajuda com")))
                .andExpect(jsonPath("$[?(@.startup.nome=='Nao Combina')].compatibilidade").value(0));
    }

    @Test
    void cadaLadoSoUsaASuaLista() throws Exception {
        Startup s = startup("S", "Fintech", Tema.PITCH);
        Mentor m = mentor("M", "Fintechs", Tema.PITCH);

        mockMvc.perform(get("/mentores/recomendados").with(Login.comoMentor(m.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/startups/recomendadas").with(Login.comoStartup(s.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLoginDa401() throws Exception {
        mockMvc.perform(get("/mentores/recomendados")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/startups/recomendadas")).andExpect(status().isUnauthorized());
    }
}
