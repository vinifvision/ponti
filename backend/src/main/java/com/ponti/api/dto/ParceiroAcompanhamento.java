package com.ponti.api.dto;

// A outra ponta do acompanhamento: o mentor (para a startup) ou a startup (para o mentor)
public record ParceiroAcompanhamento(Long id, String nome, String fotoUrl, String subtitulo) {
}
