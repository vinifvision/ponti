package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoComentario(
        @NotBlank(message = "Escreva o comentário.")
        @Size(max = 1000, message = "O comentário pode ter até 1000 caracteres.")
        String texto) {
}
