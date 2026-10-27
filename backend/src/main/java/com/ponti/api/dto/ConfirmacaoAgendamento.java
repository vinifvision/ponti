package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Corpo de POST /agendamentos/{id}/confirmar: o mentor informa o link da reunião (ex.: Google Meet)
public record ConfirmacaoAgendamento(
        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "^https://\\S+$", message = "Informe um link que comece com https://")
        String linkReuniao) {
}
