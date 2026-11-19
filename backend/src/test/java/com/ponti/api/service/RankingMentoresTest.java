package com.ponti.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.ponti.api.config.RankingPropriedades;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.service.RankingMentores.Classificado;
import com.ponti.api.service.RankingMentores.Grupo;
import com.ponti.api.service.RankingMentores.Indicadores;
import com.ponti.api.service.RankingMentores.Nota;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

// Testes de unidade da ordenação de mentores: sem banco, só as contas (como o MatchServiceTest).
// O relógio é fixo, então "hoje" é sempre o mesmo dia e a ordem dos empates é sempre a mesma.
class RankingMentoresTest {

    static final Instant AGORA = Instant.parse("2026-11-18T15:00:00Z");
    static final Clock RELOGIO = Clock.fixed(AGORA, RelogioConfig.FUSO);
    static final RankingPropriedades PADRAO = RankingPropriedades.padrao();
    static final double MEDIA_GERAL = 4.0;
    static final Long STARTUP = 1L;

    // Monta os indicadores de um mentor "comum" (com agenda, ativo, sem histórico) e deixa cada teste
    // mudar só o que importa para ele.
    static class Mentor {
        Long id;
        int compatibilidade = 80;
        Double ocupacao = 0.3;
        boolean temHorario7d = true;
        boolean temHorario14d = true;
        Double tempoResposta = null;
        int pedidosRespondidos = 0;
        Double confiabilidade = null;
        double somaNotas = 0;
        long avaliacoes = 0;
        int itensPerfil = 3;
        Instant ultimoAcesso = AGORA.minus(Duration.ofDays(1));
        Instant criadoEm = AGORA.minus(Duration.ofDays(400));
        long mentorias = 10;
        boolean recusouEstaStartup = false;

        Mentor(long id) { this.id = id; }

        Mentor match(int v) { compatibilidade = v; return this; }
        Mentor ocupacao(Double v) { ocupacao = v; return this; }
        Mentor semHorario() { temHorario7d = false; temHorario14d = false; ocupacao = null; return this; }
        Mentor avaliacoes(long n, double media) { avaliacoes = n; somaNotas = n * media; return this; }
        Mentor criadoHaDias(long dias) { criadoEm = AGORA.minus(Duration.ofDays(dias)); return this; }
        Mentor mentorias(long n) { mentorias = n; return this; }
        Mentor acessoHaDias(long dias) { ultimoAcesso = AGORA.minus(Duration.ofDays(dias)); return this; }
        Mentor recusouEstaStartup() { recusouEstaStartup = true; return this; }
        Mentor respondeEm(double horas, int pedidos) { tempoResposta = horas; pedidosRespondidos = pedidos; return this; }

        Indicadores ind() {
            return new Indicadores(id, compatibilidade, ocupacao, temHorario7d, temHorario14d, tempoResposta,
                    pedidosRespondidos, confiabilidade, somaNotas, avaliacoes, itensPerfil, ultimoAcesso,
                    criadoEm, mentorias, recusouEstaStartup);
        }
    }

    static Nota nota(Mentor m) {
        return RankingMentores.calcular(m.ind(), MEDIA_GERAL, PADRAO, RELOGIO);
    }

    static List<Long> ordem(Long startupId, Clock relogio, Mentor... mentores) {
        List<Indicadores> lista = new ArrayList<>();
        for (Mentor m : mentores) lista.add(m.ind());
        return RankingMentores.ordenar(lista, MEDIA_GERAL, startupId, PADRAO, relogio).stream()
                .map(c -> c.indicadores().mentorId()).toList();
    }

    // ---------------------------------------------------------------- cenários do documento

    @Test
    void mesmoMatch_ocupacaoAltaFicaAbaixoDeAgendaFolgada() {
        Mentor lotado = new Mentor(1).ocupacao(0.95);
        Mentor folgado = new Mentor(2).ocupacao(0.10);

        assertThat(nota(folgado).valor()).isGreaterThan(nota(lotado).valor());
        assertThat(ordem(STARTUP, RELOGIO, lotado, folgado)).containsExactly(2L, 1L);
    }

    @Test
    void media5ComUmaAvaliacaoNaoPassaMedia48Com30() {
        Mentor sortudo = new Mentor(1).avaliacoes(1, 5.0);
        Mentor consistente = new Mentor(2).avaliacoes(30, 4.8);

        // (5×4 + 5) ÷ 6 = 4,17   contra   (5×4 + 144) ÷ 35 = 4,69
        assertThat(nota(sortudo).componentes().reputacaoNota()).isCloseTo(4.167, within(0.001));
        assertThat(nota(consistente).componentes().reputacaoNota()).isCloseTo(4.686, within(0.001));
        assertThat(ordem(STARTUP, RELOGIO, sortudo, consistente)).containsExactly(2L, 1L);
    }

    @Test
    void mentorNovoSemAvaliacoesApareceEntreOsPrimeirosQuandoOMatchEBom() {
        Mentor novo = new Mentor(1).match(90).criadoHaDias(2).mentorias(0).ocupacao(0.0);
        novo.itensPerfil = 3;
        // Veteranos bem avaliados, com o mesmo match e agenda mais cheia
        Mentor[] veteranos = LongStream.rangeClosed(2, 6)
                .mapToObj(id -> new Mentor(id).match(90).ocupacao(0.5).avaliacoes(20, 4.6).respondeEm(20, 10))
                .toArray(Mentor[]::new);
        Mentor[] todos = new Mentor[veteranos.length + 1];
        todos[0] = novo;
        System.arraycopy(veteranos, 0, todos, 1, veteranos.length);

        assertThat(nota(novo).componentes().bonusNovo()).isCloseTo(0.10 * (1 - 2 / 30.0), within(1e-9));
        assertThat(nota(novo).motivos()).contains(RankingMentores.MOTIVO_NOVO);
        assertThat(ordem(STARTUP, RELOGIO, todos).subList(0, 3)).contains(1L);
    }

    @Test
    void bonusDeNovoSomeDepoisDe30DiasOuDe3Mentorias() {
        assertThat(nota(new Mentor(1).criadoHaDias(15).mentorias(0)).componentes().bonusNovo())
                .isCloseTo(0.05, within(1e-9));
        assertThat(nota(new Mentor(1).criadoHaDias(30).mentorias(0)).componentes().bonusNovo()).isZero();
        assertThat(nota(new Mentor(1).criadoHaDias(45).mentorias(0)).componentes().bonusNovo()).isZero();
        assertThat(nota(new Mentor(1).criadoHaDias(2).mentorias(3)).componentes().bonusNovo()).isZero();
        assertThat(nota(new Mentor(1).criadoHaDias(2).mentorias(3)).motivos()).doesNotContain(RankingMentores.MOTIVO_NOVO);
    }

    @Test
    void semHorarioNos14DiasVaiParaOFimMesmoComMatch100() {
        Mentor perfeitoSemAgenda = new Mentor(1).match(100).avaliacoes(40, 5.0).semHorario();
        Mentor fraco = new Mentor(2).match(10);

        assertThat(nota(perfeitoSemAgenda).grupo()).isEqualTo(Grupo.FIM_DA_LISTA);
        assertThat(nota(perfeitoSemAgenda).valor()).isGreaterThan(nota(fraco).valor());
        assertThat(ordem(STARTUP, RELOGIO, perfeitoSemAgenda, fraco)).containsExactly(2L, 1L);
    }

    @Test
    void recusouEstaStartupVaiParaOFimSoParaEla() {
        Mentor bom = new Mentor(1).match(100);
        Mentor fraco = new Mentor(2).match(10);

        // Para a startup que ele recusou: fim da lista
        assertThat(ordem(STARTUP, RELOGIO, new Mentor(1).match(100).recusouEstaStartup(), fraco))
                .containsExactly(2L, 1L);
        // Para outra startup (o indicador vem false, porque é calculado por startup): volta ao topo
        assertThat(ordem(2L, RELOGIO, bom, fraco)).containsExactly(1L, 2L);
    }

    @Test
    void inativoHaMaisDe30DiasVaiParaOFim_masAcessoDesconhecidoNao() {
        assertThat(nota(new Mentor(1).acessoHaDias(31)).grupo()).isEqualTo(Grupo.FIM_DA_LISTA);
        assertThat(nota(new Mentor(1).acessoHaDias(29)).grupo()).isEqualTo(Grupo.PRINCIPAL);
        Mentor desconhecido = new Mentor(1);
        desconhecido.ultimoAcesso = null;
        assertThat(nota(desconhecido).grupo()).isEqualTo(Grupo.PRINCIPAL);
    }

    @Test
    void camposNulosUsamOValorNeutroENaoLancamExcecao() {
        Mentor antigo = new Mentor(1).match(60);
        antigo.ocupacao = null;
        antigo.tempoResposta = null;
        antigo.confiabilidade = null;
        antigo.ultimoAcesso = null;
        antigo.criadoEm = null;
        antigo.mentorias = 0;

        Nota n = nota(antigo);

        assertThat(n.componentes().ocupacao()).isEqualTo(0.5);
        assertThat(n.componentes().confiabilidade()).isEqualTo(0.5); // tempo 0,5 e confiabilidade 0,5
        assertThat(n.componentes().atividade()).isEqualTo(0.5);
        assertThat(n.componentes().bonusNovo()).isZero();          // cadastro sem data: sem bônus
        assertThat(n.componentes().reputacao()).isEqualTo(0.75);   // sem avaliações = média geral (4,0)
        assertThat(n.valor()).isGreaterThan(0);
        // Mesmo com a startup nula (não deveria acontecer) não quebra
        assertThat(RankingMentores.ordenar(List.of(antigo.ind()), MEDIA_GERAL, null, PADRAO, RELOGIO)).hasSize(1);
    }

    @Test
    void comRelogioFixoAOrdemDosEmpatesESempreAMesma_eMudaComADataOuAStartup() {
        // 20 mentores idênticos: todos na mesma faixa, a ordem sai só do sorteio
        Mentor[] iguais = LongStream.rangeClosed(1, 20).mapToObj(Mentor::new).toArray(Mentor[]::new);

        List<Long> hoje = ordem(STARTUP, RELOGIO, iguais);
        assertThat(ordem(STARTUP, RELOGIO, iguais)).isEqualTo(hoje);
        // Mais tarde no mesmo dia: mesma ordem
        assertThat(ordem(STARTUP, Clock.offset(RELOGIO, Duration.ofHours(5)), iguais)).isEqualTo(hoje);
        // Outro dia ou outra startup: ordem diferente
        assertThat(ordem(STARTUP, Clock.offset(RELOGIO, Duration.ofDays(1)), iguais)).isNotEqualTo(hoje);
        assertThat(ordem(2L, RELOGIO, iguais)).isNotEqualTo(hoje);
        // E não é simplesmente a ordem dos ids
        assertThat(hoje).isNotEqualTo(LongStream.rangeClosed(1, 20).boxed().toList());
    }

    @Test
    void desempateSoMexeDentroDaFaixa_diferencaMaiorQueAFaixaSempreVence() {
        Mentor melhor = new Mentor(1).match(85);   // 0,45 × 0,05 = 0,0225 de vantagem (> 0,02)
        Mentor pior = new Mentor(2).match(80);
        for (long dia = 0; dia < 30; dia++) {
            Clock outroDia = Clock.offset(RELOGIO, Duration.ofDays(dia));
            assertThat(ordem(STARTUP, outroDia, pior, melhor)).containsExactly(1L, 2L);
        }
    }

    // ---------------------------------------------------------------- componentes e motivos

    @Test
    void tempoDeRespostaEAtividadeSaoLinearesEntreOsLimites() {
        assertThat(RankingMentores.fatorTempoResposta(6.0)).isEqualTo(1.0);
        assertThat(RankingMentores.fatorTempoResposta(42.0)).isEqualTo(0.5);
        assertThat(RankingMentores.fatorTempoResposta(100.0)).isEqualTo(0.0);
        assertThat(nota(new Mentor(1).acessoHaDias(3)).componentes().atividade()).isEqualTo(1.0);
        assertThat(nota(new Mentor(1).acessoHaDias(30)).componentes().atividade()).isEqualTo(0.0);
    }

    @Test
    void mediaGeralSemNenhumaAvaliacaoE4() {
        assertThat(RankingMentores.mediaGeral(0, 0)).isEqualTo(4.0);
        assertThat(RankingMentores.mediaGeral(46, 10)).isEqualTo(4.6);
    }

    @Test
    void motivosSaoNoMaximoDoisNaOrdemDePrioridade() {
        Mentor completo = new Mentor(1).respondeEm(5, 8).avaliacoes(10, 4.9).criadoHaDias(5).mentorias(1);

        assertThat(nota(completo).motivos())
                .containsExactly(RankingMentores.MOTIVO_HORARIOS, RankingMentores.MOTIVO_RESPONDE);
    }

    @Test
    void motivosExigemHistoricoMinimo() {
        // Responde rápido, mas só com 2 pedidos; bem avaliado, mas só com 4 avaliações
        Mentor pouco = new Mentor(1).respondeEm(2, 2).avaliacoes(4, 5.0);
        pouco.temHorario7d = false;

        assertThat(nota(pouco).motivos()).isEmpty();
    }

    @Test
    void bemAvaliadoUsaAReputacaoAjustada() {
        Mentor m = new Mentor(1).avaliacoes(30, 4.9);
        m.temHorario7d = false;

        assertThat(nota(m).motivos()).containsExactly(RankingMentores.MOTIVO_BEM_AVALIADO);
    }

    @Test
    void notasProximasCaemNaMesmaFaixa() {
        assertThat(RankingMentores.faixa(0.812, 0.02)).isEqualTo(RankingMentores.faixa(0.819, 0.02));
        assertThat(RankingMentores.faixa(0.60, 0.02)).isEqualTo(30);
    }

    @Test
    void ordenarDevolveANotaJuntoComOMentor() {
        List<Classificado> lista = RankingMentores.ordenar(List.of(new Mentor(7).ind()), MEDIA_GERAL, STARTUP, PADRAO, RELOGIO);
        assertThat(lista.get(0).indicadores().mentorId()).isEqualTo(7L);
        assertThat(lista.get(0).nota().grupo()).isEqualTo(Grupo.PRINCIPAL);
    }
}
