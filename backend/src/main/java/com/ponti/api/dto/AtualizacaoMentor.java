package com.ponti.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import com.ponti.api.model.Tema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

// Corpo de PATCH /mentores/{id}: só os campos enviados mudam; os que ficam de fora continuam iguais.
// Texto vazio ("") apaga o campo.
public record AtualizacaoMentor(
        @Size(min = 2, max = 120) String nome,
        @Size(max = 2000) String bio,
        @Size(max = 255) String areasAtuacao,
        @Size(max = 255) String ecossistemas,
        // Lista vazia apaga; ausente mantém
        @Size(max = 5) Set<Tema> temas,
        @Min(0) @Max(70) Integer anosExperiencia,
        @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal valorSessao,
        @Min(15) @Max(240) Integer duracaoSessaoMinutos,
        @Size(max = 500) String fotoUrl,
        @Size(max = 500) String videoUrl,
        @Size(max = 500) String linkedinUrl) {
}
