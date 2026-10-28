package com.ponti.api.dto;

import com.ponti.api.model.MetodoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

// Corpo de POST /agendamentos. O preço e o fim da sessão vêm do perfil do mentor, não do front.
// Mentoria paga: metodoPagamento é obrigatório; com CARTAO, os dados do cartão também.
public record NovoAgendamento(
        @NotNull Long startupId,
        @NotNull Long mentorId,
        @NotNull LocalDateTime inicio,
        MetodoPagamento metodoPagamento,
        @Valid DadosCartao cartao,
        @Size(max = 1000) String mensagem) {

    // Atalho usado nos testes e pelo Pix: sem dados de cartão
    public NovoAgendamento(Long startupId, Long mentorId, LocalDateTime inicio,
                           MetodoPagamento metodoPagamento, String mensagem) {
        this(startupId, mentorId, inicio, metodoPagamento, null, mensagem);
    }
}
