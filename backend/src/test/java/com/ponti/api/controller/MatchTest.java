package com.ponti.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Tema;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Match pela API: cada lado vê a lista do outro. A startup recebe os mentores na ordem de recomendação
// (RankingMentores), em que a compatibilidade pesa mais, mas quem não tem agenda vai para o fim.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchTest {

    @Autowired MockMvc mockMvc;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired DisponibilidadeRepository disponibilidadeRepository;
    @Autowired AgendamentoRepository agendamentoRepository;

    Mentor mentor(String nome, String setores, Tema... temas) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setEcossistemas(setores);
        m.setTemas(new LinkedHashSet<>(List.of(temas)));
        m.setValorSessao(BigDecimal.ZERO);
        m.setDuracaoSessaoMinutos(30);
        m = mentorRepository.save(m);
        // Agenda aberta todos os dias, para o mentor ficar no grupo principal do ranking
        for (DayOfWeek dia : DayOfWeek.values()) {
            Disponibilidade d = new Disponibilidade();
            d.setMentorId(m.getId());
            d.setDiaSemana(dia);
            d.setHoraInicio(LocalTime.of(0, 0));
            d.setHoraFim(LocalTime.of(23, 30));
            disponibilidadeRepository.save(d);
        }
        return m;
    }

    Mentor mentorSemAgenda(String nome, String setores, Tema... temas) {
        Mentor m = mentor(nome, setores, temas);
        disponibilidadeRepository.deleteByMentorId(m.getId());
        return m;
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
                .andExpect(jsonPath("$.itens[0].mentor.nome").value("Mentor Perfeito"))
                .andExpect(jsonPath("$.itens[0].compatibilidade").value(100))
                .andExpect(jsonPath("$.itens[0].motivos[0]").value(org.hamcrest.Matchers.startsWith("Ajuda com")))
                .andExpect(jsonPath("$.itens[1].mentor.nome").value("Mentor Parcial"))
                .andExpect(jsonPath("$.itens[1].compatibilidade").value(50)) // 37,5 dos temas + 12,5 do setor, arredondado
                .andExpect(jsonPath("$.itens[?(@.mentor.nome=='Mentor Fora')].compatibilidade").value(0));
    }

    @Test
    void mentorSemAgendaVaiParaOFimMesmoSendoOMaisCompativel() throws Exception {
        Startup s = startup("FinY", "Fintech", Tema.JURIDICO, Tema.GESTAO);
        mentorSemAgenda("Perfeito Sem Agenda", "Fintechs", Tema.JURIDICO, Tema.GESTAO);
        mentor("Parcial Com Agenda", "Fintechs", Tema.JURIDICO);

        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(s.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].mentor.nome").value("Parcial Com Agenda"))
                .andExpect(jsonPath("$.itens[0].motivos").value(org.hamcrest.Matchers.hasItem("Tem horários esta semana")))
                // Continua na lista, com a compatibilidade dele; só deixa de disputar o topo
                .andExpect(jsonPath("$.itens[-1:].mentor.nome").value("Perfeito Sem Agenda"))
                .andExpect(jsonPath("$.itens[-1:].compatibilidade").value(100));
    }

    @Test
    void mentorQueRecusouAStartupVaiParaOFimSoParaEla() throws Exception {
        Startup recusada = startup("Recusada", "Fintech", Tema.JURIDICO);
        Startup outra = startup("Outra", "Fintech", Tema.JURIDICO);
        Mentor m = mentor("Mentor Que Recusou", "Fintechs", Tema.JURIDICO);

        Agendamento a = new Agendamento();
        a.setStartupId(recusada.getId());
        a.setMentorId(m.getId());
        a.setInicio(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS));
        a.setFim(a.getInicio().plusMinutes(30));
        a.setValor(BigDecimal.ZERO);
        a.setStatus(StatusAgendamento.RECUSADO);
        a.setStatusPagamento(StatusPagamento.GRATUITA);
        a.setRespondidoEm(Instant.now().minus(2, ChronoUnit.DAYS));
        agendamentoRepository.save(a);

        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(recusada.getId())))
                .andExpect(jsonPath("$.itens[0].mentor.nome").value(org.hamcrest.Matchers.not("Mentor Que Recusou")))
                .andExpect(jsonPath("$.itens[-1:].mentor.nome").value("Mentor Que Recusou"));
        mockMvc.perform(get("/mentores/recomendados").with(Login.comoStartup(outra.getId())))
                .andExpect(jsonPath("$.itens[0].mentor.nome").value("Mentor Que Recusou"));
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
