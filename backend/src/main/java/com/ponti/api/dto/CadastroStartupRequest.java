package com.ponti.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroStartupRequest(
        @NotBlank @Size(min = 3, max = 50) String usuario,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotBlank String nome,
        String descricao,
        String segmento,
        String cidade,
        String estado,
        @Min(1) @Max(9) Integer estagioMaturidade) {
}
