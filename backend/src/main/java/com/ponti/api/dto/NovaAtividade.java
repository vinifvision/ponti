package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record NovaAtividade(
        @NotBlank(message = "Informe o título da atividade.")
        @Size(max = 200, message = "O título pode ter até 200 caracteres.")
        String titulo,
        @Size(max = 2000, message = "A descrição pode ter até 2000 caracteres.")
        String descricao,
        @NotNull(message = "Escolha o prazo de entrega.")
        // Para o envio com arquivos (multipart), que chega como texto: "2026-11-19T23:59:00"
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime prazo) {
}
