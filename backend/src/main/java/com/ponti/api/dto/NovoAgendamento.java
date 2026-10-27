package com.ponti.api.dto;

import com.ponti.api.model.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

// Corpo de POST /agendamentos. O preço e o fim da sessão vêm do perfil do mentor, não do front.
// metodoPagamento é obrigatório quando a mentoria é paga (pagamento simulado).
public record NovoAgendamento(
        @NotNull Long startupId,
        @NotNull Long mentorId,
        @NotNull LocalDateTime inicio,
        MetodoPagamento metodoPagamento,
        @Size(max = 1000) String mensagem) {
}
