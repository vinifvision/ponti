package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.*;
import com.ponti.api.repository.*;
import com.ponti.api.service.DashboardService;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Dashboard do admin com um cenário pequeno, montado aqui, em que cada número foi conferido à mão.
// O banco é esvaziado no início de cada teste (dentro da transação, que é desfeita no fim).
//
// Cenário (período padrão: últimos 30 dias):
//   Mentores: M1 (Marketing e Vendas, E-commerce), M2 (Jurídico), M3 (Gestão, mal avaliado)
//   S1, há 25 dias: precisa de Marketing e Vendas, E-commerce. Pediu mentoria ao M1 (100%), confirmada e
//                   realizada, e está em acompanhamento com ele.
//   S2, há 10 dias: precisa de Marketing. Convidou o M2 (0%), que recusou. Pediu mentoria ao M2, sem resposta.
//   S3, há 20 dias: só se cadastrou.
//   S4, há 40 dias (período anterior): precisa de Jurídico. Convite ao M2 (75%) aceito; acompanhamento parado.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTest {

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired ConviteRepository conviteRepository;
    @Autowired AgendamentoRepository agendamentoRepository;
    @Autowired AvaliacaoRepository avaliacaoRepository;
    @Autowired AcompanhamentoRepository acompanhamentoRepository;
    @Autowired AtividadeRepository atividadeRepository;
    @Autowired EntregaRepository entregaRepository;
    @Autowired MetaRepository metaRepository;
    @Autowired ComentarioRepository comentarioRepository;
    @Autowired DisponibilidadeRepository disponibilidadeRepository;
    @Autowired CertificacaoRepository certificacaoRepository;
    @Autowired AnexoRepository anexoRepository;
    @Autowired RegistroAuditoriaRepository auditoriaRepository;

    LocalDateTime agora = LocalDateTime.now(RelogioConfig.FUSO);
    Mentor m1, m2, m3;
    Startup s1, s2, s3, s4;
    Usuario u2, u3;

    @BeforeEach
    void montarCenario() {
        comentarioRepository.deleteAll();
        anexoRepository.deleteAll();
        entregaRepository.deleteAll();
        atividadeRepository.deleteAll();
        metaRepository.deleteAll();
        acompanhamentoRepository.deleteAll();
        avaliacaoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        conviteRepository.deleteAll();
        disponibilidadeRepository.deleteAll();
        certificacaoRepository.deleteAll();
        auditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        startupRepository.deleteAll();
        mentorRepository.deleteAll();

        m1 = mentor("M1", "E-commerce", Tema.MARKETING, Tema.VENDAS);
        m2 = mentor("M2", "Saúde", Tema.JURIDICO);
        m3 = mentor("M3", "Saúde", Tema.GESTAO);
        s1 = startup("S1", "E-commerce", 25, Tema.MARKETING, Tema.VENDAS);
        s2 = startup("S2", "Fintech", 10, Tema.MARKETING);
        s3 = startup("S3", "Fintech", 20, Tema.VENDAS);
        s4 = startup("S4", "Govtech", 40, Tema.JURIDICO);
        u2 = usuarioRepository.findAll().stream().filter(u -> s2.getId().equals(u.getStartupId())).findFirst().orElseThrow();
        u3 = usuarioRepository.findAll().stream().filter(u -> s3.getId().equals(u.getStartupId())).findFirst().orElseThrow();

        // S1 + M1: pedido há 4 dias, confirmado, sessão há 2 dias; acompanhamento ativo e andando
        sessao(s1, m1, agora.minusDays(2), StatusAgendamento.CONFIRMADO, diasAtras(4));
        Acompanhamento a1 = acompanhamento(s1, m1, 20);
        Atividade noPrazo = atividade(a1, -5, StatusAtividade.APROVADA);
        entrega(noPrazo, -6);
        Atividade atrasada = atividade(a1, -3, StatusAtividade.ENTREGUE);
        entrega(atrasada, -1);
        atividade(a1, -2, StatusAtividade.PENDENTE); // vencida

        // S2 + M2: convite recusado (match baixo) e pedido sem resposta há 5 dias
        convite(s2, m2, StatusConvite.RECUSADO, diasAtras(9), diasAtras(8));
        sessao(s2, m2, agora.plusDays(3), StatusAgendamento.PENDENTE, diasAtras(5));

        // S4 + M2: convite aceito há 37 dias (período anterior); acompanhamento parado há 30 dias
        convite(s4, m2, StatusConvite.ACEITO, diasAtras(38), diasAtras(37));
        acompanhamento(s4, m2, 30);

        // M3: duas mentorias antigas (fora dos dois períodos) com notas 2 e 3
        avaliar(sessao(s4, m3, agora.minusDays(75), StatusAgendamento.CONFIRMADO, diasAtras(80)), 2);
        avaliar(sessao(s4, m3, agora.minusDays(70), StatusAgendamento.CONFIRMADO, diasAtras(72)), 3);
    }

    ResultActions dashboard() throws Exception {
        return mockMvc.perform(get("/admin/dashboard").with(Login.comoAdmin(1L)));
    }

    @Nested
    class Funil {

        @Test
        void contaAsStartupsCadastradasNoPeriodoEmCadaEtapa() throws Exception {
            dashboard().andExpect(status().isOk())
                    .andExpect(jsonPath("$.funil.etapas[0].etapa").value("CADASTRO"))
                    .andExpect(jsonPath("$.funil.etapas[0].quantidade").value(3))     // S1, S2, S3
                    .andExpect(jsonPath("$.funil.etapas[1].quantidade").value(2))     // S1, S2
                    .andExpect(jsonPath("$.funil.etapas[1].taxaDaAnterior").value(67))
                    .andExpect(jsonPath("$.funil.etapas[2].quantidade").value(1))     // S1
                    .andExpect(jsonPath("$.funil.etapas[2].taxaDaAnterior").value(50))
                    .andExpect(jsonPath("$.funil.etapas[2].taxaDoInicio").value(33))
                    .andExpect(jsonPath("$.funil.etapas[3].quantidade").value(1))
                    .andExpect(jsonPath("$.funil.etapas[4].etapa").value("ACOMPANHAMENTO"))
                    .andExpect(jsonPath("$.funil.etapas[4].quantidade").value(1))
                    .andExpect(jsonPath("$.funil.maiorQueda").value("MATCH"));
        }

        @Test
        void comparaComOPeriodoAnterior() throws Exception {
            dashboard()
                    .andExpect(jsonPath("$.funil.etapas[0].quantidadeAnterior").value(1))   // S4
                    .andExpect(jsonPath("$.funil.etapas[2].quantidadeAnterior").value(1))   // convite aceito
                    .andExpect(jsonPath("$.funil.etapas[3].quantidadeAnterior").value(1))   // mentorias com o M3
                    .andExpect(jsonPath("$.funil.etapas[4].quantidadeAnterior").value(1))
                    .andExpect(jsonPath("$.destaques[0].chave").value("startups"))
                    .andExpect(jsonPath("$.destaques[0].atual").value(3))
                    .andExpect(jsonPath("$.destaques[0].anterior").value(1))
                    .andExpect(jsonPath("$.destaques[2].chave").value("matches"))
                    .andExpect(jsonPath("$.destaques[2].atual").value(1))
                    .andExpect(jsonPath("$.destaques[2].anterior").value(1))
                    .andExpect(jsonPath("$.destaques[3].chave").value("mentorias"))
                    .andExpect(jsonPath("$.destaques[3].atual").value(1));
        }
    }

    @Nested
    class ProvaDoMatch {

        @Test
        void separaOsParesPelaCompatibilidade() throws Exception {
            dashboard()
                    .andExpect(jsonPath("$.match.faixas[0].faixa").value("ALTA"))
                    .andExpect(jsonPath("$.match.faixas[0].pares").value(1))        // S1+M1 (100%)
                    .andExpect(jsonPath("$.match.faixas[0].taxa").value(100))
                    .andExpect(jsonPath("$.match.faixas[1].pares").value(0))
                    .andExpect(jsonPath("$.match.faixas[1].taxa").doesNotExist())   // sem base: nulo, não 0
                    .andExpect(jsonPath("$.match.faixas[2].faixa").value("BAIXA"))
                    .andExpect(jsonPath("$.match.faixas[2].pares").value(1))        // S2+M2 (0%), convite e pedido = 1 par
                    .andExpect(jsonPath("$.match.faixas[2].taxa").value(0))
                    .andExpect(jsonPath("$.match.faixas[0].paresAnterior").value(1)) // S4+M2 (75%)
                    .andExpect(jsonPath("$.match.diferenca").value(100));
        }

        @Test
        void limitesDasFaixas() {
            assertThat(faixa(100)).isEqualTo("ALTA");
            assertThat(faixa(70)).isEqualTo("ALTA");
            assertThat(faixa(69)).isEqualTo("MEDIA");
            assertThat(faixa(40)).isEqualTo("MEDIA");
            assertThat(faixa(39)).isEqualTo("BAIXA");
            assertThat(faixa(0)).isEqualTo("BAIXA");
        }
    }

    @Nested
    class OfertaEDemanda {

        @Test
        void mostraPrimeiroOsTemasEmQueFaltaMentor() throws Exception {
            dashboard()
                    .andExpect(jsonPath("$.ofertaDemanda[0].tema").value("MARKETING"))
                    .andExpect(jsonPath("$.ofertaDemanda[0].startups").value(2))
                    .andExpect(jsonPath("$.ofertaDemanda[0].mentores").value(1))
                    .andExpect(jsonPath("$.ofertaDemanda[0].situacao").value("FALTA"))
                    .andExpect(jsonPath("$.ofertaDemanda[?(@.tema == 'GESTAO')].situacao").value("SOBRA"))
                    .andExpect(jsonPath("$.ofertaDemanda[?(@.tema == 'JURIDICO')].situacao").value("EQUILIBRIO"))
                    .andExpect(jsonPath("$.ofertaDemanda[?(@.tema == 'PITCH')]").isEmpty()); // ninguém: não aparece
        }

        @Test
        void contaBloqueadaNaoEntraNaOferta() throws Exception {
            u2.bloquear("Teste", Instant.now());
            usuarioRepository.save(u2);
            dashboard().andExpect(jsonPath("$.ofertaDemanda[?(@.tema == 'MARKETING')].startups").value(1));
        }

        @Test
        void regraDaSituacao() {
            assertThat(situacao(2, 1)).isEqualTo("FALTA");
            assertThat(situacao(3, 2)).isEqualTo("FALTA");
            assertThat(situacao(1, 0)).isEqualTo("FALTA");
            assertThat(situacao(1, 1)).isEqualTo("EQUILIBRIO");
            assertThat(situacao(2, 3)).isEqualTo("EQUILIBRIO");
            assertThat(situacao(1, 2)).isEqualTo("SOBRA");
            assertThat(situacao(0, 1)).isEqualTo("SOBRA");
        }
    }

    @Nested
    class SaudeDosAcompanhamentos {

        @Test
        void contaEntregasNoPrazoAtrasadasEAcompanhamentosParados() throws Exception {
            dashboard()
                    .andExpect(jsonPath("$.saude.ativos").value(2))
                    .andExpect(jsonPath("$.saude.entregasNoPrazo").value(1))
                    .andExpect(jsonPath("$.saude.entregasAtrasadas").value(1))
                    .andExpect(jsonPath("$.saude.atividadesVencidas").value(1))
                    .andExpect(jsonPath("$.saude.percentualNoPrazo").value(50))
                    .andExpect(jsonPath("$.saude.parados.length()").value(1))   // S1 teve entrega ontem
                    .andExpect(jsonPath("$.saude.parados[0].startup").value("S4"))
                    .andExpect(jsonPath("$.saude.parados[0].mentor").value("M2"))
                    .andExpect(jsonPath("$.saude.parados[0].diasSemMovimento").value(30));
        }
    }

    @Nested
    class PrecisaDeAtencao {

        @Test
        void listaMentorMalAvaliadoMentorSemRespostaEStartupSemConexao() throws Exception {
            dashboard()
                    .andExpect(jsonPath("$.atencao.length()").value(3))
                    .andExpect(jsonPath("$.atencao[0].tipo").value("MENTOR_MAL_AVALIADO"))
                    .andExpect(jsonPath("$.atencao[0].nome").value("M3"))
                    .andExpect(jsonPath("$.atencao[0].motivo").value("Nota média 2,5 em 2 avaliações"))
                    .andExpect(jsonPath("$.atencao[1].tipo").value("MENTOR_SEM_RESPOSTA"))
                    .andExpect(jsonPath("$.atencao[1].nome").value("M2"))
                    .andExpect(jsonPath("$.atencao[1].motivo").value("1 pedido sem resposta há 5 dias"))
                    .andExpect(jsonPath("$.atencao[2].tipo").value("STARTUP_SEM_CONEXAO"))
                    .andExpect(jsonPath("$.atencao[2].nome").value("S3"))   // S2 só tem 10 dias
                    .andExpect(jsonPath("$.atencao[2].usuarioId").value(u3.getId()))
                    .andExpect(jsonPath("$.atencao[2].motivo")
                            .value("Cadastrada há 20 dias e ainda sem conexão (não pediu nenhuma mentoria)"));
        }

        @Test
        void contaBloqueadaSaiDaLista() throws Exception {
            u3.bloquear("Teste", Instant.now());
            usuarioRepository.save(u3);
            dashboard().andExpect(jsonPath("$.atencao.length()").value(2));
        }
    }

    @Nested
    class PeriodoEAcesso {

        @Test
        void periodoPadraoSaoOsUltimos30DiasEOAnteriorTemOMesmoTamanho() throws Exception {
            LocalDate hoje = agora.toLocalDate();
            dashboard()
                    .andExpect(jsonPath("$.periodo.fim").value(hoje.toString()))
                    .andExpect(jsonPath("$.periodo.inicio").value(hoje.minusDays(29).toString()))
                    .andExpect(jsonPath("$.periodo.anteriorFim").value(hoje.minusDays(30).toString()))
                    .andExpect(jsonPath("$.periodo.anteriorInicio").value(hoje.minusDays(59).toString()));
        }

        @Test
        void periodoEscolhidoMudaOsNumeros() throws Exception {
            // Só os últimos 15 dias: entra apenas a S2 no cadastro
            mockMvc.perform(get("/admin/dashboard").with(Login.comoAdmin(1L))
                            .param("inicio", agora.toLocalDate().minusDays(14).toString())
                            .param("fim", agora.toLocalDate().toString()))
                    .andExpect(jsonPath("$.funil.etapas[0].quantidade").value(1))
                    .andExpect(jsonPath("$.funil.etapas[0].quantidadeAnterior").value(2)); // S1 e S3
        }

        @Test
        void periodoInvalidoDa400() throws Exception {
            mockMvc.perform(get("/admin/dashboard").with(Login.comoAdmin(1L))
                    .param("inicio", "2026-11-20").param("fim", "2026-11-01")).andExpect(status().isBadRequest());
            mockMvc.perform(get("/admin/dashboard").with(Login.comoAdmin(1L))
                    .param("inicio", "2024-01-01").param("fim", "2026-11-01")).andExpect(status().isBadRequest());
        }

        @Test
        void soOAdminVe() throws Exception {
            mockMvc.perform(get("/admin/dashboard")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/admin/dashboard").with(Login.comoStartup(s1.getId()))).andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/dashboard").with(Login.comoMentor(m1.getId()))).andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------- apoio

    static String faixa(int compatibilidade) {
        return DashboardService.faixa(compatibilidade);
    }

    static String situacao(int startups, int mentores) {
        return DashboardService.situacao(startups, mentores);
    }

    Instant diasAtras(int dias) {
        return agora.minusDays(dias).atZone(RelogioConfig.FUSO).toInstant();
    }

    Mentor mentor(String nome, String setores, Tema... temas) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setEcossistemas(setores);
        m.setTemas(Set.of(temas));
        m.setValorSessao(BigDecimal.ZERO);
        m.setDuracaoSessaoMinutos(60);
        m = mentorRepository.save(m);
        Usuario u = new Usuario();
        u.setEmail(nome.toLowerCase() + "@teste.com");
        u.setSenhaHash("x");
        u.setTipo(TipoUsuario.MENTOR);
        u.setMentorId(m.getId());
        u.setCriadoEm(diasAtras(100));
        usuarioRepository.save(u);
        return m;
    }

    Startup startup(String nome, String segmento, int cadastroHaDias, Tema... necessidades) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setSegmento(segmento);
        s.setNecessidades(Set.of(necessidades));
        s = startupRepository.save(s);
        Usuario u = new Usuario();
        u.setEmail(nome.toLowerCase() + "@teste.com");
        u.setSenhaHash("x");
        u.setTipo(TipoUsuario.STARTUP);
        u.setStartupId(s.getId());
        u.setCriadoEm(diasAtras(cadastroHaDias));
        usuarioRepository.save(u);
        return s;
    }

    Agendamento sessao(Startup s, Mentor m, LocalDateTime inicio, StatusAgendamento status, Instant pedidoEm) {
        Agendamento a = new Agendamento();
        a.setStartupId(s.getId());
        a.setMentorId(m.getId());
        a.setInicio(inicio);
        a.setFim(inicio.plusHours(1));
        a.setValor(BigDecimal.ZERO);
        a.setStatus(status);
        a.setStatusPagamento(StatusPagamento.GRATUITA);
        a.setCriadoEm(pedidoEm);
        if (status == StatusAgendamento.CONFIRMADO) a.setRespondidoEm(pedidoEm.plus(1, ChronoUnit.HOURS));
        return agendamentoRepository.save(a);
    }

    void convite(Startup s, Mentor m, StatusConvite status, Instant enviadoEm, Instant respondidoEm) {
        Convite c = new Convite();
        c.setStartupId(s.getId());
        c.setMentorId(m.getId());
        c.setEnviadoPor(TipoUsuario.STARTUP);
        c.setStatus(status);
        c.setCriadoEm(enviadoEm);
        c.setRespondidoEm(respondidoEm);
        conviteRepository.save(c);
    }

    void avaliar(Agendamento a, int nota) {
        Avaliacao av = new Avaliacao();
        av.setAgendamentoId(a.getId());
        av.setMentorId(a.getMentorId());
        av.setStartupId(a.getStartupId());
        av.setNota(nota);
        av.setCriadoEm(a.getFim().atZone(RelogioConfig.FUSO).toInstant());
        avaliacaoRepository.save(av);
    }

    Acompanhamento acompanhamento(Startup s, Mentor m, int inicioHaDias) {
        Acompanhamento a = new Acompanhamento();
        a.setStartupId(s.getId());
        a.setMentorId(m.getId());
        a.setObjetivo("Objetivo de teste");
        a.setStatus(StatusAcompanhamento.ATIVO);
        a.setDataInicio(agora.toLocalDate().minusDays(inicioHaDias));
        a.setCriadoEm(diasAtras(inicioHaDias + 1));
        return acompanhamentoRepository.save(a);
    }

    Atividade atividade(Acompanhamento a, int prazoEmDias, StatusAtividade status) {
        Atividade at = new Atividade();
        at.setAcompanhamentoId(a.getId());
        at.setTitulo("Atividade de teste");
        at.setPrazo(agora.plusDays(prazoEmDias));
        at.setStatus(status);
        at.setCriadaEm(diasAtras(20));
        return atividadeRepository.save(at);
    }

    void entrega(Atividade at, int emDias) {
        Entrega e = new Entrega();
        e.setAtividadeId(at.getId());
        e.setTexto("Entrega de teste");
        e.setEnviadaEm(agora.plusDays(emDias));
        entregaRepository.save(e);
    }
}
