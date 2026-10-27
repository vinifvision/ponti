package com.ponti.api.dto;

import com.ponti.api.model.MetodoPagamento;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Agendamento com os dados da startup e do mentor, para as telas de Mentorias dos dois lados.
// O link da reunião só aparece quando a sessão está CONFIRMADA.
public record AgendamentoDetalhe(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fim,
        BigDecimal valor,
        StatusAgendamento status,
        StatusPagamento statusPagamento,
        MetodoPagamento metodoPagamento,
        String linkReuniao,
        String mensagem,
        Parte startup,
        Parte mentor) {

    // Resumo de cada lado: nome, foto e uma linha de descrição (segmento ou área de atuação)
    public record Parte(Long id, String nome, String fotoUrl, String descricao) {
    }
}
