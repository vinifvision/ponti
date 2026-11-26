package com.ponti.api.dto;

import java.time.LocalDateTime;
import java.util.List;

// Formato de "Entrega" em acompanhamento.models.ts
public record EntregaResposta(
        Long id,
        String texto,
        String link,
        List<AnexoResposta> anexos,
        LocalDateTime enviadaEm,
        String feedback,
        List<ComentarioResposta> comentarios) {
}
