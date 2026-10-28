package com.ponti.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corpo de POST /agendamentos/{id}/avaliacao
public record NovaAvaliacao(
        @NotNull @Min(1) @Max(5) Integer nota,
        @Size(max = 1000) String comentario) {
}
