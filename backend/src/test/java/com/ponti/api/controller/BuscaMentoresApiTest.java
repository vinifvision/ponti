package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Tema;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Busca de mentores pela API (etapa 5): filtros, ordenação e paginação no servidor.
// Os mentores deste teste atendem a área "Spacetech", que só eles têm: o filtro area=Spacetech separa
// eles dos mentores de exemplo que o DadosIniciais cria.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BuscaMentoresApiTest {

    static final String AREA = "Spacetech";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired DisponibilidadeRepository disponibilidadeRepository;

    Long startupId;
    Long mentorId;

    @BeforeEach
    void preparar() {
        Startup s = new Startup();
        s.setNome("Startup da Busca");
        s.setSegmento("Spacetech");
        s.setNecessidades(new LinkedHashSet<>(List.of(Tema.PITCH)));
        startupId = startupRepository.save(s).getId();

        // 15 mentores: os de número par são sêniores (10 anos), os ímpares iniciantes (1 ano)
        for (int i = 1; i <= 15; i++) {
            Mentor m = novoMentor("Mentor Busca " + String.format("%02d", i), i % 2 == 0 ? 10 : 1,
                    new BigDecimal(i * 20), true);
            if (i == 1) mentorId = m.getId();
        }
        novoMentor("Mentor Busca Sem Agenda", 10, new BigDecimal("80"), false);
    }

    Mentor novoMentor(String nome, int anos, BigDecimal valor, boolean comAgenda) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setEcossistemas(AREA + ", Aeroespacial");
        m.setTemas(new LinkedHashSet<>(List.of(Tema.PITCH)));
        m.setAnosExperiencia(anos);
        m.setValorSessao(valor);
        m.setDuracaoSessaoMinutos(60);
        m = mentorRepository.save(m);
        if (comAgenda) {
            for (DayOfWeek dia : DayOfWeek.values()) {
                Disponibilidade d = new Disponibilidade();
                d.setMentorId(m.getId());
                d.setDiaSemana(dia);
                d.setHoraInicio(LocalTime.of(0, 0));
                d.setHoraFim(LocalTime.of(23, 0));
                disponibilidadeRepository.save(d);
            }
        }
        return m;
    }

    JsonNode json(MockHttpServletRequestBuilder req) throws Exception {
        String corpo = mockMvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    List<String> nomes(JsonNode pagina, boolean recomendados) {
        List<String> nomes = new ArrayList<>();
        pagina.get("itens").forEach(i -> nomes.add((recomendados ? i.get("mentor") : i).get("nome").asText()));
        return nomes;
    }

    // ---------------------------------------------------------------- startup: /mentores/recomendados

    @Test
    void filtrosEPaginacao_totalConsideraOsFiltrosEAPagina1NaoRepeteAPagina0() throws Exception {
        // 7 sêniores com agenda (2, 4, ..., 14) + 1 sênior sem agenda = 8 sêniores na área
        JsonNode p0 = json(get("/mentores/recomendados").with(Login.comoStartup(startupId))
                .param("area", AREA).param("experiencia", "senior").param("tamanho", "5"));
        JsonNode p1 = json(get("/mentores/recomendados").with(Login.comoStartup(startupId))
                .param("area", AREA).param("experiencia", "senior").param("tamanho", "5").param("pagina", "1"));

        assertThat(p0.get("totalItens").asInt()).isEqualTo(8);
        assertThat(p0.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(p0.get("pagina").asInt()).isZero();
        assertThat(p0.get("tamanho").asInt()).isEqualTo(5);
        assertThat(nomes(p0, true)).hasSize(5);
        assertThat(nomes(p1, true)).hasSize(3).doesNotContainAnyElementsOf(nomes(p0, true));
        // O sem agenda continua na lista, só que no fim
        assertThat(nomes(p1, true).get(2)).isEqualTo("Mentor Busca Sem Agenda");
    }

    @Test
    void semParametrosDevolveAPrimeiraPaginaCom12() throws Exception {
        JsonNode p = json(get("/mentores/recomendados").with(Login.comoStartup(startupId)));

        assertThat(p.get("tamanho").asInt()).isEqualTo(12);
        assertThat(p.get("itens").size()).isEqualTo(12);
        assertThat(p.get("totalItens").asInt()).isGreaterThanOrEqualTo(16);
        assertThat(p.get("itens").get(0).has("compatibilidade")).isTrue();
    }

    @Test
    void comHorarioEm7DiasTiraQuemNaoTemAgenda() throws Exception {
        JsonNode p = json(get("/mentores/recomendados").with(Login.comoStartup(startupId))
                .param("area", AREA).param("comHorarioEm7Dias", "true").param("tamanho", "50"));

        assertThat(p.get("totalItens").asInt()).isEqualTo(15);
        assertThat(nomes(p, true)).doesNotContain("Mentor Busca Sem Agenda");
    }

    @Test
    void ordenacaoPorMenorValor() throws Exception {
        JsonNode p = json(get("/mentores/recomendados").with(Login.comoStartup(startupId))
                .param("area", AREA).param("ordenacao", "menor-valor").param("tamanho", "3"));

        // Valores: mentor i cobra i × 20 (o sem agenda cobra 80)
        assertThat(nomes(p, true)).containsExactly("Mentor Busca 01", "Mentor Busca 02", "Mentor Busca 03");
    }

    @Test
    void buscaPorTextoIgnoraAcento() throws Exception {
        JsonNode p = json(get("/mentores/recomendados").with(Login.comoStartup(startupId))
                .param("q", "AEROESPACÍAL").param("tamanho", "50"));

        assertThat(p.get("totalItens").asInt()).isEqualTo(16);
    }

    @Test
    void parametroInvalidoDa400NoFormatoPadraoDeErro() throws Exception {
        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(startupId)).param("faixaPreco", "barata"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("gratuita, ate-100, 100-200, acima-200")));
        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(startupId)).param("tamanho", "51"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(startupId)).param("pagina", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mentorContinuaSemAcessoAosRecomendados() throws Exception {
        mockMvc.perform(get("/mentores/recomendados").with(Login.comoMentor(mentorId)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- geral: /mentores

    @Test
    void listaGeralEPaginadaEmOrdemDeNomeComOsMesmosFiltros() throws Exception {
        JsonNode p = json(get("/mentores").with(Login.comoMentor(mentorId))
                .param("area", AREA).param("experiencia", "iniciante").param("tamanho", "3"));

        assertThat(p.get("totalItens").asInt()).isEqualTo(8); // 1, 3, ..., 15
        assertThat(nomes(p, false)).containsExactly("Mentor Busca 01", "Mentor Busca 03", "Mentor Busca 05");
        assertThat(p.get("itens").get(0).has("notaMedia")).isTrue(); // MentorResposta, com a reputação
    }

    @Test
    void listaGeralNaoAceitaOrdenacaoRecomendados() throws Exception {
        mockMvc.perform(get("/mentores").with(Login.comoMentor(mentorId)).param("ordenacao", "recomendados"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("só para startups")));
    }

    @Test
    void listaGeralFiltraPorHorarioNaSemana() throws Exception {
        JsonNode p = json(get("/mentores").with(Login.comoMentor(mentorId))
                .param("area", AREA).param("comHorarioEm7Dias", "true").param("tamanho", "50"));

        assertThat(p.get("totalItens").asInt()).isEqualTo(15);
    }

    // ---------------------------------------------------------------- áreas

    @Test
    void areasTrazTodasAsAreasMesmoAsQueNaoCabemNumaPagina() throws Exception {
        mockMvc.perform(get("/mentores/areas").with(Login.comoStartup(startupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasItems("Aeroespacial", AREA, "Health-techs", "PetTech")));
    }

    @Test
    void areasExigeLogin() throws Exception {
        mockMvc.perform(get("/mentores/areas")).andExpect(status().isUnauthorized());
    }
}
