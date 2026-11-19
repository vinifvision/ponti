package com.ponti.api.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Parâmetros da ordenação de mentores (ponti.ranking.* no application.properties).
// Ficam fora do código para dar para ajustar a ordem olhando os dados de demonstração.
//
// A nota de ordenação é:
//   pesoMatch × M + pesoOcupacao × (1 − O) + pesoReputacao × R + pesoConfiabilidade × C
//   + pesoPerfil × P + pesoAtividade × A + bônus de mentor novo
// Os seis pesos precisam somar 1. Se não somarem, a aplicação NÃO sobe: o construtor abaixo lança
// a exceção quando o Spring lê as propriedades, e a mensagem diz o que corrigir.
@ConfigurationProperties("ponti.ranking")
public record RankingPropriedades(
        double pesoMatch,
        double pesoOcupacao,
        double pesoReputacao,
        double pesoConfiabilidade,
        double pesoPerfil,
        double pesoAtividade,
        // Bônus máximo de mentor novo e quando ele acaba (dias de cadastro ou mentorias realizadas)
        double bonusNovo,
        int diasNovo,
        int mentoriasNovo,
        // Janela da ocupação e da regra "sem horário" (fim da lista)
        int janelaAgendaDias,
        // Dias sem acesso para ir ao fim da lista
        int inativoDias,
        // Janela da regra "recusou esta startup" (fim da lista só para ela)
        int recusaDias,
        // Constante da média bayesiana: quantas "avaliações médias" cada mentor começa tendo
        int priorAvaliacoes,
        // Largura da faixa de desempate: notas na mesma faixa são embaralhadas pela rotação diária
        double faixaDesempate) {

    // Folga para erros de arredondamento de double (0.45 + 0.15 + ... pode dar 0.9999999999)
    static final double TOLERANCIA_SOMA = 1e-6;

    public RankingPropriedades {
        List<String> erros = new ArrayList<>();

        double[] pesos = {pesoMatch, pesoOcupacao, pesoReputacao, pesoConfiabilidade, pesoPerfil, pesoAtividade};
        for (double peso : pesos) {
            if (peso < 0) {
                erros.add("os pesos não podem ser negativos");
                break;
            }
        }
        double soma = somaDosPesos(pesos);
        if (Math.abs(soma - 1.0) > TOLERANCIA_SOMA) {
            erros.add("os pesos (peso-match, peso-ocupacao, peso-reputacao, peso-confiabilidade, peso-perfil, "
                    + "peso-atividade) precisam somar 1, mas somam " + soma);
        }
        if (bonusNovo < 0) erros.add("bonus-novo não pode ser negativo");
        if (diasNovo <= 0) erros.add("dias-novo precisa ser maior que zero");
        if (mentoriasNovo <= 0) erros.add("mentorias-novo precisa ser maior que zero");
        if (janelaAgendaDias <= 0) erros.add("janela-agenda-dias precisa ser maior que zero");
        if (inativoDias <= 0) erros.add("inativo-dias precisa ser maior que zero");
        if (recusaDias <= 0) erros.add("recusa-dias precisa ser maior que zero");
        if (priorAvaliacoes < 0) erros.add("prior-avaliacoes não pode ser negativo");
        if (faixaDesempate <= 0 || faixaDesempate > 1) erros.add("faixa-desempate precisa estar entre 0 e 1");

        if (!erros.isEmpty()) {
            throw new IllegalArgumentException("Configuração ponti.ranking inválida: " + String.join("; ", erros));
        }
    }

    // Os valores padrão do documento de busca e ordenação. São os mesmos do application.properties
    // (o RankingPropriedadesTest confere); servem para os testes de unidade, que não sobem o Spring.
    public static RankingPropriedades padrao() {
        return new RankingPropriedades(0.45, 0.15, 0.15, 0.15, 0.05, 0.05,
                0.10, 30, 3, 14, 30, 30, 5, 0.02);
    }

    private static double somaDosPesos(double[] pesos) {
        double soma = 0;
        for (double peso : pesos) soma += peso;
        return soma;
    }
}
