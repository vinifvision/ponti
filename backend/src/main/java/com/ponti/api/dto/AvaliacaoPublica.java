package com.ponti.api.dto;

import java.time.Instant;

// Avaliação como aparece no perfil do mentor
public record AvaliacaoPublica(Long id, Integer nota, String comentario,
                               String startupNome, String startupFotoUrl, Instant criadoEm) {
}
