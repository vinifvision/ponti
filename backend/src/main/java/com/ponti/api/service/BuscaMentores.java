package com.ponti.api.service;

import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.Pagina;
import java.math.BigDecimal;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// BUSCA de mentores: filtros, ordenação e paginação feitos no servidor (GET /mentores e /mentores/recomendados).
//
// As regras reproduzem as que a tela "Encontrar mentores" aplicava no navegador (mentoresFiltrados,
// experiencia.utils.ts e preco.utils.ts), para a troca não mudar nada para o usuário. A única diferença
// é que aqui a busca por texto e a área também ignoram acentos ("saude" acha "Saúde").
//
// Funções puras, sem banco: o controller monta o Filtro e os services aplicam sobre a lista de mentores.
public final class BuscaMentores {

    private BuscaMentores() {
    }

    public static final int TAMANHO_PADRAO = 12;
    public static final int TAMANHO_MAXIMO = 50;

    // Comparação de nomes em português ("Álvaro" fica junto de "Alberto", não depois de "Zé")
    private static final Collator COLLATOR = Collator.getInstance(Locale.forLanguageTag("pt-BR"));

    // ---------------------------------------------------------------- opções dos filtros

    // Faixas do filtro "Experiência" (anos inteiros). Sem anosExperiencia o mentor não entra em nenhuma.
    public enum Experiencia {
        INICIANTE("iniciante"),          // até 2 anos
        INTERMEDIARIO("intermediario"),  // de 3 a 5 anos
        SENIOR("senior");                // mais de 5 anos

        final String valor;
        Experiencia(String valor) { this.valor = valor; }

        boolean atende(Integer anos) {
            if (anos == null) return false;
            return switch (this) {
                case INICIANTE -> anos <= 2;
                case INTERMEDIARIO -> anos > 2 && anos <= 5;
                case SENIOR -> anos > 5;
            };
        }
    }

    // Faixas do filtro "Faixa de preço" (valorSessao em reais). Gratuita é só R$ 0; as pagas começam acima de 0.
    // Sem valorSessao ("A combinar") o mentor não entra em nenhuma.
    public enum FaixaPreco {
        GRATUITA("gratuita"),
        ATE_100("ate-100"),
        DE_100_A_200("100-200"),
        ACIMA_200("acima-200");

        final String valor;
        FaixaPreco(String valor) { this.valor = valor; }

        private static final BigDecimal CEM = new BigDecimal("100");
        private static final BigDecimal DUZENTOS = new BigDecimal("200");

        boolean atende(BigDecimal preco) {
            if (preco == null) return false;
            return switch (this) {
                case GRATUITA -> preco.signum() == 0;
                case ATE_100 -> preco.signum() > 0 && preco.compareTo(CEM) <= 0;
                case DE_100_A_200 -> preco.compareTo(CEM) > 0 && preco.compareTo(DUZENTOS) <= 0;
                case ACIMA_200 -> preco.compareTo(DUZENTOS) > 0;
            };
        }
    }

    // "recomendados" usa a nota do RankingMentores; as outras ordenam pelo campo, com a nota como desempate
    public enum Ordenacao {
        RECOMENDADOS("recomendados"),
        NOME("nome"),
        MENOR_VALOR("menor-valor"),
        MAIOR_VALOR("maior-valor");

        final String valor;
        Ordenacao(String valor) { this.valor = valor; }
    }

    // ---------------------------------------------------------------- filtro

    // Tudo o que veio na URL, já validado. Campos de texto vazios viram null (sem filtro).
    public record Filtro(String q, String area, Experiencia experiencia, FaixaPreco faixaPreco,
                         boolean comHorarioEm7Dias, Ordenacao ordenacao, int pagina, int tamanho) {

        // Monta o filtro a partir dos parâmetros da URL. Valor inválido dá 400 com a lista do que é aceito.
        public static Filtro de(String q, String area, String experiencia, String faixaPreco, Boolean comHorarioEm7Dias,
                                String ordenacao, Integer pagina, Integer tamanho, Ordenacao ordenacaoPadrao) {
            int p = pagina == null ? 0 : pagina;
            int t = tamanho == null ? TAMANHO_PADRAO : tamanho;
            if (p < 0) {
                throw invalido("A página começa em 0");
            }
            if (t < 1 || t > TAMANHO_MAXIMO) {
                throw invalido("O tamanho da página deve ser de 1 a " + TAMANHO_MAXIMO);
            }
            Ordenacao o = vazio(ordenacao) ? ordenacaoPadrao : opcao(Ordenacao.values(), ordenacao, "ordenacao");
            return new Filtro(
                    vazio(q) ? null : q.trim(),
                    vazio(area) ? null : area.trim(),
                    vazio(experiencia) ? null : opcao(Experiencia.values(), experiencia, "experiencia"),
                    vazio(faixaPreco) ? null : opcao(FaixaPreco.values(), faixaPreco, "faixaPreco"),
                    Boolean.TRUE.equals(comHorarioEm7Dias),
                    o, p, t);
        }

        // O mentor passa em todos os filtros? (temHorario7d vem dos indicadores do ranking)
        public boolean aceita(MentorResposta m, boolean temHorario7d) {
            return passaBusca(m) && passaArea(m)
                    && (experiencia == null || experiencia.atende(m.anosExperiencia()))
                    && (faixaPreco == null || faixaPreco.atende(m.valorSessao()))
                    && (!comHorarioEm7Dias || temHorario7d);
        }

        // Contém o termo em nome, bio, áreas de atuação ou ecossistemas
        private boolean passaBusca(MentorResposta m) {
            if (q == null) return true;
            String texto = String.join(" ", textos(m.nome(), m.bio(), m.areasAtuacao(), m.ecossistemas()));
            return normalizar(texto).contains(normalizar(q));
        }

        // Um dos itens de ecossistemas é igual à área escolhida
        private boolean passaArea(MentorResposta m) {
            if (area == null) return true;
            String alvo = normalizar(area);
            return separarLista(m.ecossistemas()).stream().anyMatch(e -> normalizar(e).equals(alvo));
        }
    }

    // ---------------------------------------------------------------- ordenação por campo

    // Ordem de uma ordenação por campo (nome ou valor). Para RECOMENDADOS devolve null: a lista já vem na
    // ordem do ranking. Use com uma ordenação ESTÁVEL (List.sort), para os empates manterem a ordem anterior
    // (a do ranking, nos recomendados; a do nome, na lista geral).
    // Como na tela, "A combinar" (sem valor) conta como R$ 0 nas ordenações por valor.
    public static <T> Comparator<T> porCampo(Ordenacao ordenacao, Function<T, MentorResposta> mentor) {
        return switch (ordenacao) {
            case RECOMENDADOS -> null;
            case NOME -> Comparator.comparing(t -> mentor.apply(t).nome(), Comparator.nullsLast(COLLATOR::compare));
            case MENOR_VALOR -> Comparator.comparing(t -> valorParaOrdenar(mentor.apply(t)));
            case MAIOR_VALOR -> Comparator.comparing((T t) -> valorParaOrdenar(mentor.apply(t))).reversed();
        };
    }

    // Aplica o filtro, a ordenação por campo e a paginação sobre uma lista que já está na ordem base
    public static <T> Pagina<T> buscar(List<T> naOrdemBase, Filtro filtro, Predicate<T> passaNoFiltro,
                                                         Function<T, MentorResposta> mentor) {
        List<T> resultado = new ArrayList<>(naOrdemBase.stream().filter(passaNoFiltro).toList());
        Comparator<T> ordem = porCampo(filtro.ordenacao(), mentor);
        if (ordem != null) {
            resultado.sort(ordem); // estável: empates mantêm a ordem base
        }
        return Pagina.de(resultado, filtro.pagina(), filtro.tamanho());
    }

    // ---------------------------------------------------------------- áreas

    // Todas as áreas (itens de ecossistemas) dos mentores, sem repetir, em ordem alfabética
    public static List<String> areas(List<String> ecossistemasDosMentores) {
        Set<String> areas = new LinkedHashSet<>();
        ecossistemasDosMentores.forEach(e -> areas.addAll(separarLista(e)));
        return areas.stream().sorted(COLLATOR::compare).toList();
    }

    // ---------------------------------------------------------------- apoio

    // Separa "Fintechs, SaaS" em ["Fintechs", "SaaS"] (mesma regra do separarLista do front)
    static List<String> separarLista(String valor) {
        if (valor == null) return List.of();
        return Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    // Minúsculas e sem acento, para comparar "Saúde" com "saude"
    static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static BigDecimal valorParaOrdenar(MentorResposta m) {
        return m.valorSessao() == null ? BigDecimal.ZERO : m.valorSessao();
    }

    private static List<String> textos(String... partes) {
        return Arrays.stream(partes).map(p -> p == null ? "" : p).toList();
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    // Acha a opção pelo valor da URL ("menor-valor" -> MENOR_VALOR), sem diferenciar maiúsculas
    private static <E extends Enum<E>> E opcao(E[] opcoes, String valor, String parametro) {
        String procurado = valor.trim().toLowerCase(Locale.ROOT);
        for (E opcao : opcoes) {
            if (valorDe(opcao).equals(procurado)) return opcao;
        }
        String aceitos = String.join(", ", Arrays.stream(opcoes).map(BuscaMentores::valorDe).toList());
        throw invalido("Valor inválido para " + parametro + ": \"" + valor.trim() + "\". Use um destes: " + aceitos);
    }

    private static String valorDe(Enum<?> opcao) {
        if (opcao instanceof Experiencia e) return e.valor;
        if (opcao instanceof FaixaPreco f) return f.valor;
        return ((Ordenacao) opcao).valor;
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
