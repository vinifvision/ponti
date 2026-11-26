package com.ponti.api.dto;

import java.time.Instant;

public record MetaResposta(Long id, String titulo, boolean cumprida, Instant cumpridaEm) {
}
