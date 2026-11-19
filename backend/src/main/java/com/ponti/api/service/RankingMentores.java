package com.ponti.api.service;

import com.ponti.api.config.RankingPropriedades;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// ORDENAÇÃO dos mentores recomendados para uma startup (documento "Busca e ordenação de mentores").
//
// A ordem sai de três camadas, nesta sequência:
//   1. Grupo: o mentor vai para o FIM DA LISTA se não tem horário livre nos próximos 14 dias, se não
//      acessa a plataforma há mais de 30 dias ou se recusou um pedido desta startup nos últimos 30 dias.
//      O grupo principal sempre vem antes.
//   2. Nota de ordenação (maior primeiro), uma soma ponderada de 0 a 1 mais o bônus de mentor novo:
//        nota = pesoMatch×M + pesoOcupacao×(1−O) + pesoReputacao×R + pesoConfiabilidade×C
//             + pesoPerfil×P + pesoAtividade×A + B
//   3. Desempate com rotação: notas muito próximas caem na mesma faixa (largura faixaDesempate) e, dentro
//      dela, a ordem vem de um "sorteio" fixo por (mentor, startup, dia). A mesma startup vê a mesma
//      ordem o dia todo, e startups diferentes não recebem exatamente o mesmo topo.
//
// Tudo aqui é função pura: recebe os indicadores já coletados, os parâmetros e o relógio, e devolve a nota.
// Não usa banco nem Spring, então dá para testar direto (RankingMentoresTest), como o MatchService.calcular.
//
// Regra dos dados que faltam: componente sem dado (mentor sem histórico ou registro antigo) usa o VALOR
// NEUTRO, nunca zero. Assim ninguém é punido por falta de dado.
public final class RankingMentores {

    private RankingMentores() {
    }

    static final double NEUTRO = 0.5;
    // Média da plataforma usada quando ainda não existe nenhuma avaliação
    static final double MEDIA_GERAL_SEM_AVALIACOES = 4.0;
    // Tempo de resposta: até 12 h vale 1, 72 h ou mais vale 0, linear entre eles
    static final double RESPOSTA_OTIMA_HORAS = 12;
    static final double RESPOSTA_RUIM_HORAS = 72;
    // Atividade: acesso nos últimos 7 dias vale 1; com inativoDias ou mais, vale 0
    static final double ATIVO_DIAS = 7;
    // Itens do perfil completo: foto, vídeo, bio, LinkedIn e ao menos uma certificação
    static final int ITENS_PERFIL = 5;

    // Motivos de ordenação (sempre positivos; a lista nunca explica por que alguém desceu)
    static final String MOTIVO_HORARIOS = "Tem horários esta semana";
    static final String MOTIVO_RESPONDE = "Costuma responder em até 24h";
    static final String MOTIVO_BEM_AVALIADO = "Bem avaliado";
    static final String MOTIVO_NOVO = "Novo na Ponti";
    static final int MAXIMO_MOTIVOS = 2;
    static final double RESPONDE_RAPIDO_HORAS = 24;
    static final int RESPONDE_RAPIDO_MINIMO_PEDIDOS = 3;
    static final double BEM_AVALIADO_NOTA = 4.5;
    static final int BEM_AVALIADO_MINIMO_AVALIACOES = 5;

    // O que se sabe de um mentor para ordenar. Campos que podem faltar são objetos (null = sem dado).
    public record Indicadores(
            Long mentorId,
            // Compatibilidade do MatchService, de 0 a 100
            int compatibilidade,
            // Horas reservadas ÷ horas oferecidas na janela, de 0 a 1 (null = não oferece horas)
            Double ocupacao,
            boolean temHorario7d,
            boolean temHorario14d,
            // Mediana do tempo de resposta dos últimos pedidos e quantos pedidos entraram na conta
            Double tempoRespostaMedianoHoras,
            int pedidosRespondidos,
            // 1 − (recusas + cancelamentos do mentor) ÷ pedidos recebidos, últimos 90 dias (null = sem pedidos)
            Double taxaConfiabilidade,
            // Soma e quantidade das notas recebidas (1 a 5), para a média bayesiana
            double somaNotas,
            long totalAvaliacoes,
            // Quantos dos 5 itens do perfil estão preenchidos
            int itensPerfil,
            Instant ultimoAcessoEm,
            Instant criadoEm,
            long mentoriasRealizadas,
            boolean recusouEstaStartup) {
    }

    public enum Grupo { PRINCIPAL, FIM_DA_LISTA }

    // Cada componente de 0 a 1 (bônus de 0 a bonusNovo) e a reputação na escala de 1 a 5
    public record Componentes(double match, double ocupacao, double reputacao, double confiabilidade,
                              double perfil, double atividade, double bonusNovo, double reputacaoNota) {
    }

    public record Nota(double valor, Grupo grupo, long faixa, Componentes componentes, List<String> motivos) {
    }

    public record Classificado(Indicadores indicadores, Nota nota) {
    }

    // ---------------------------------------------------------------- ordenação

    // Ordena a lista inteira para a startup: grupo, faixa da nota (maior primeiro) e rotação diária
    public static List<Classificado> ordenar(List<Indicadores> mentores, double mediaGeral, Long startupId,
                                             RankingPropriedades p, Clock relogio) {
        LocalDate hoje = LocalDate.now(relogio);
        return mentores.stream()
                .map(i -> new Classificado(i, calcular(i, mediaGeral, p, relogio)))
                .sorted(Comparator.comparing((Classificado c) -> c.nota().grupo())
                        .thenComparing(c -> c.nota().faixa(), Comparator.reverseOrder())
                        .thenComparingLong(c -> sorteio(c.indicadores().mentorId(), startupId, hoje))
                        .thenComparing(c -> c.indicadores().mentorId()))
                .toList();
    }

    // Número "aleatório" mas fixo para (mentor, startup, dia): muda de um dia para o outro e de uma
    // startup para a outra, mas não muda entre duas buscas da mesma startup no mesmo dia.
    static long sorteio(Long mentorId, Long startupId, LocalDate dia) {
        long x = valor(mentorId) * 0x9E3779B97F4A7C15L
                ^ valor(startupId) * 0xC2B2AE3D27D4EB4FL
                ^ dia.toEpochDay() * 0x165667B19E3779F9L;
        // Embaralha os bits (finalizador do SplitMix64): ids vizinhos não ficam em posições vizinhas
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    // ---------------------------------------------------------------- nota de um mentor

    public static Nota calcular(Indicadores i, double mediaGeral, RankingPropriedades p, Clock relogio) {
        Instant agora = relogio.instant();

        double m = limitar(i.compatibilidade() / 100.0);
        // Sem horas oferecidas não há ocupação para medir: o mentor já vai para o fim da lista pela regra
        // "sem horário", e aqui usa o neutro para não pesar duas vezes.
        double o = i.ocupacao() == null ? NEUTRO : limitar(i.ocupacao());
        double reputacaoNota = reputacaoAjustada(i.somaNotas(), i.totalAvaliacoes(), mediaGeral, p.priorAvaliacoes());
        double r = limitar((reputacaoNota - 1) / 4);
        double c = (fatorTempoResposta(i.tempoRespostaMedianoHoras())
                + (i.taxaConfiabilidade() == null ? NEUTRO : limitar(i.taxaConfiabilidade()))) / 2;
        double perfil = limitar((double) i.itensPerfil() / ITENS_PERFIL);
        double a = fatorAtividade(i.ultimoAcessoEm(), agora, p.inativoDias());
        double b = bonusNovo(i.criadoEm(), i.mentoriasRealizadas(), agora, p);

        double valor = p.pesoMatch() * m
                + p.pesoOcupacao() * (1 - o)
                + p.pesoReputacao() * r
                + p.pesoConfiabilidade() * c
                + p.pesoPerfil() * perfil
                + p.pesoAtividade() * a
                + b;

        Componentes componentes = new Componentes(m, o, r, c, perfil, a, b, reputacaoNota);
        return new Nota(valor, grupo(i, agora, p), faixa(valor, p.faixaDesempate()),
                componentes, motivos(i, componentes));
    }

    static Grupo grupo(Indicadores i, Instant agora, RankingPropriedades p) {
        boolean semHorario = !i.temHorario14d();
        // Último acesso desconhecido (registro antigo): a regra não se aplica
        boolean inativo = i.ultimoAcessoEm() != null && dias(i.ultimoAcessoEm(), agora) > p.inativoDias();
        return semHorario || inativo || i.recusouEstaStartup() ? Grupo.FIM_DA_LISTA : Grupo.PRINCIPAL;
    }

    // Faixa de desempate: com largura 0,02, notas 0,812 e 0,819 caem na mesma faixa (40)
    static long faixa(double nota, double largura) {
        // A folga mínima evita que 0,6 ÷ 0,02 = 29,999999... caia na faixa de baixo
        return (long) Math.floor(nota / largura + 1e-9);
    }

    // Média bayesiana, na escala de 1 a 5: começa perto da média da plataforma e só se afasta dela
    // conforme o mentor acumula avaliações. Assim, um 5,0 com 1 avaliação não passa um 4,8 com 30.
    static double reputacaoAjustada(double somaNotas, long totalAvaliacoes, double mediaGeral, int prior) {
        return (prior * mediaGeral + somaNotas) / (prior + totalAvaliacoes);
    }

    // Média de todas as notas da plataforma (4,0 enquanto ninguém foi avaliado)
    public static double mediaGeral(double somaDeTodasAsNotas, long totalDeAvaliacoes) {
        return totalDeAvaliacoes == 0 ? MEDIA_GERAL_SEM_AVALIACOES : somaDeTodasAsNotas / totalDeAvaliacoes;
    }

    static double fatorTempoResposta(Double medianaHoras) {
        if (medianaHoras == null) return NEUTRO;
        return linearDecrescente(medianaHoras, RESPOSTA_OTIMA_HORAS, RESPOSTA_RUIM_HORAS);
    }

    static double fatorAtividade(Instant ultimoAcesso, Instant agora, int inativoDias) {
        if (ultimoAcesso == null) return NEUTRO;
        return linearDecrescente(dias(ultimoAcesso, agora), ATIVO_DIAS, inativoDias);
    }

    // bonusNovo × (1 − dias de cadastro ÷ diasNovo), só enquanto tiver menos de mentoriasNovo mentorias
    static double bonusNovo(Instant criadoEm, long mentoriasRealizadas, Instant agora, RankingPropriedades p) {
        if (criadoEm == null || mentoriasRealizadas >= p.mentoriasNovo()) return 0;
        return p.bonusNovo() * Math.max(0, 1 - dias(criadoEm, agora) / p.diasNovo());
    }

    // No máximo dois, nesta ordem de prioridade
    static List<String> motivos(Indicadores i, Componentes c) {
        List<String> motivos = new ArrayList<>();
        if (i.temHorario7d()) {
            motivos.add(MOTIVO_HORARIOS);
        }
        if (i.tempoRespostaMedianoHoras() != null && i.tempoRespostaMedianoHoras() <= RESPONDE_RAPIDO_HORAS
                && i.pedidosRespondidos() >= RESPONDE_RAPIDO_MINIMO_PEDIDOS) {
            motivos.add(MOTIVO_RESPONDE);
        }
        if (c.reputacaoNota() >= BEM_AVALIADO_NOTA && i.totalAvaliacoes() >= BEM_AVALIADO_MINIMO_AVALIACOES) {
            motivos.add(MOTIVO_BEM_AVALIADO);
        }
        if (c.bonusNovo() > 0) {
            motivos.add(MOTIVO_NOVO);
        }
        return List.copyOf(motivos.subList(0, Math.min(MAXIMO_MOTIVOS, motivos.size())));
    }

    // ---------------------------------------------------------------- apoio

    // 1 até "otimo", 0 a partir de "ruim", linear entre os dois
    private static double linearDecrescente(double valor, double otimo, double ruim) {
        if (valor <= otimo) return 1;
        if (valor >= ruim) return 0;
        return 1 - (valor - otimo) / (ruim - otimo);
    }

    // Dias (com fração) entre o instante e agora; datas no futuro contam como 0
    private static double dias(Instant desde, Instant agora) {
        return Math.max(0, Duration.between(desde, agora).toMinutes() / (24.0 * 60));
    }

    private static double limitar(double valor) {
        return Math.max(0, Math.min(1, valor));
    }

    private static long valor(Long id) {
        return id == null ? 0 : id;
    }
}
