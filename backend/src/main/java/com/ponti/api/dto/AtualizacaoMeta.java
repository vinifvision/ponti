package com.ponti.api.dto;

import jakarta.validation.constraints.NotNull;

// O mentor marca (ou desmarca) uma meta como cumprida
public record AtualizacaoMeta(
        @NotNull(message = "Informe se a meta foi cumprida.")
        Boolean cumprida) {
}
