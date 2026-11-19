package com.ponti.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

// Indicadores pré-calculados de cada mentor, usados pela ordenação dos mentores recomendados
// (ver RankingMentores). Uma linha por mentor. A busca só lê esta tabela, em vez de calcular agenda,
// respostas e avaliações de todos os mentores a cada requisição.
//
// É recalculada pelo IndicadoresMentorService em dois momentos:
//   - a cada 15 minutos, para todos (cobre a passagem do tempo: horários que vencem, janela que anda);
//   - na hora, só do mentor afetado, quando um agendamento muda, a agenda muda ou entra uma avaliação.
//
// O que depende da startup que busca (match, "recusou esta startup") ou é barato (perfil completo,
// último acesso, data de cadastro) é calculado na hora da busca e não fica aqui.
@Entity
public class MentorIndicadores {

    // O mesmo id do Mentor (não é gerado)
    @Id
    private Long mentorId;

    // Horas reservadas ÷ horas oferecidas na janela da agenda, de 0 a 1 (null = não oferece horas)
    private Double ocupacao;

    private boolean temHorario7d;
    private boolean temHorario14d;

    // Mediana do tempo de resposta dos últimos pedidos (null = nunca respondeu) e quantos entraram na conta
    private Double tempoRespostaMedianoHoras;
    private int pedidosRespondidos;

    // 1 − (recusas + cancelamentos do mentor) ÷ pedidos recebidos nos últimos 90 dias (null = sem pedidos)
    private Double taxaConfiabilidade;

    // Soma e quantidade das notas recebidas. A reputação ajustada (média bayesiana) é calculada na busca,
    // porque depende da média de TODA a plataforma, que muda a cada avaliação de qualquer mentor.
    private double somaNotas;
    private long totalAvaliacoes;

    private long mentoriasRealizadas;

    private Instant atualizadoEm;

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Double getOcupacao() { return ocupacao; }
    public void setOcupacao(Double ocupacao) { this.ocupacao = ocupacao; }
    public boolean isTemHorario7d() { return temHorario7d; }
    public void setTemHorario7d(boolean temHorario7d) { this.temHorario7d = temHorario7d; }
    public boolean isTemHorario14d() { return temHorario14d; }
    public void setTemHorario14d(boolean temHorario14d) { this.temHorario14d = temHorario14d; }
    public Double getTempoRespostaMedianoHoras() { return tempoRespostaMedianoHoras; }
    public void setTempoRespostaMedianoHoras(Double tempoRespostaMedianoHoras) { this.tempoRespostaMedianoHoras = tempoRespostaMedianoHoras; }
    public int getPedidosRespondidos() { return pedidosRespondidos; }
    public void setPedidosRespondidos(int pedidosRespondidos) { this.pedidosRespondidos = pedidosRespondidos; }
    public Double getTaxaConfiabilidade() { return taxaConfiabilidade; }
    public void setTaxaConfiabilidade(Double taxaConfiabilidade) { this.taxaConfiabilidade = taxaConfiabilidade; }
    public double getSomaNotas() { return somaNotas; }
    public void setSomaNotas(double somaNotas) { this.somaNotas = somaNotas; }
    public long getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(long totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }
    public long getMentoriasRealizadas() { return mentoriasRealizadas; }
    public void setMentoriasRealizadas(long mentoriasRealizadas) { this.mentoriasRealizadas = mentoriasRealizadas; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
