package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// O mentor propõe um acompanhamento: objetivo e até 5 metas iniciais (opcionais)
public record NovaProposta(
        @NotNull(message = "Escolha a startup.")
        Long startupId,
        @NotBlank(message = "Escreva o objetivo do acompanhamento.")
        @Size(max = 500, message = "O objetivo pode ter até 500 caracteres.")
        String objetivo,
        @Size(max = 5, message = "Você pode sugerir até 5 metas.")
        List<@Size(max = 200, message = "Cada meta pode ter até 200 caracteres.") String> metas) {
}
