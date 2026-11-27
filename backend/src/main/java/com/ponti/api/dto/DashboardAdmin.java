package com.ponti.api.dto;

import com.ponti.api.model.Tema;
import com.ponti.api.model.TipoUsuario;
import java.time.LocalDate;
import java.util.List;

// Resposta de GET /admin/dashboard. Cada bloco responde uma pergunta do admin:
//   destaques     -> "A plataforma cresceu em relação ao período anterior?"
//   funil         -> "Em que etapa as startups travam?"
//   match         -> "Match alto vira mentoria mais vezes?" (prova de que a recomendação funciona)
//   ofertaDemanda -> "Em que tema faltam mentores?"
//   saude         -> "Os acompanhamentos estão andando?"
//   atencao       -> "Com quem eu preciso falar?"
// Taxas são porcentagens inteiras (0 a 100); vêm nulas quando não há base para calcular (divisão por zero).
public record DashboardAdmin(
        Periodo periodo,
        List<Destaque> destaques,
        Funil funil,
        ProvaDoMatch match,
        List<TemaOfertaDemanda> ofertaDemanda,
        Saude saude,
        List<Atencao> atencao) {

    public record Periodo(LocalDate inicio, LocalDate fim, LocalDate anteriorInicio, LocalDate anteriorFim) {
    }

    public record Destaque(String chave, String rotulo, long atual, long anterior) {
    }

    // maiorQueda: a etapa que menos recebe da anterior (onde o admin deve agir primeiro)
    public record Funil(List<EtapaFunil> etapas, String maiorQueda) {
    }

    public record EtapaFunil(String etapa, String rotulo, long quantidade, Integer taxaDaAnterior,
                             Integer taxaDoInicio, long quantidadeAnterior, Integer taxaDaAnteriorNoPeriodoAnterior) {
    }

    // diferenca: taxa da faixa alta menos a da faixa baixa, em pontos percentuais
    public record ProvaDoMatch(List<FaixaMatch> faixas, Integer diferenca) {
    }

    public record FaixaMatch(String faixa, String rotulo, int pares, int convertidos, Integer taxa,
                             int paresAnterior, Integer taxaAnterior) {
    }

    // situacao: FALTA (muita startup para pouco mentor), EQUILIBRIO ou SOBRA
    public record TemaOfertaDemanda(Tema tema, String rotulo, int startups, int mentores, String situacao) {
    }

    public record Saude(int ativos, int entregasNoPrazo, int entregasAtrasadas, int atividadesVencidas,
                        Integer percentualNoPrazo, List<AcompanhamentoParado> parados) {
    }

    public record AcompanhamentoParado(Long acompanhamentoId, String startup, String mentor, long diasSemMovimento) {
    }

    // tipo: MENTOR_MAL_AVALIADO, MENTOR_SEM_RESPOSTA ou STARTUP_SEM_CONEXAO
    public record Atencao(String tipo, Long usuarioId, TipoUsuario perfil, Long perfilId, String nome, String motivo,
                          boolean bloqueado) {
    }
}
