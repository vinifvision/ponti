package com.ponti.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CadastroMentorRequest(
        @NotBlank @Size(min = 3, max = 50) String usuario,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotBlank String nome,
        String bio,
        String areasAtuacao,
        String ecossistemas,
        @DecimalMin("0") BigDecimal valorSessao) {
}
