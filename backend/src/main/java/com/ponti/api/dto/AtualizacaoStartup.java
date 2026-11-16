package com.ponti.api.dto;

import com.ponti.api.model.Tema;
import com.ponti.api.model.Whatsapp;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

// Corpo de PATCH /startups/{id}: só os campos enviados mudam; os que ficam de fora continuam iguais.
// Texto vazio ("") apaga o campo.
public record AtualizacaoStartup(
        @Size(min = 2, max = 120) String nome,
        @Size(max = 2000) String descricao,
        @Size(max = 500) String videoPitchUrl,
        @Size(max = 500) String fotoUrl,
        @Size(max = 2000) String atuacoes,
        @Size(max = 255) String segmento,
        @Size(max = 120) String cidade,
        @Size(max = 2) String estado,
        @Size(max = 500) String site,
        @Min(1) @Max(9) Integer estagioMaturidade,
        // Lista vazia apaga; ausente mantém
        @Size(max = 3) Set<Tema> necessidades,
        // Opcional para a startup: "" apaga o número
        @Pattern(regexp = "(" + Whatsapp.FORMATO + ")?", message = "WhatsApp inválido: informe DDD + número")
        String whatsapp,
        Boolean compartilharWhatsapp) {

    public AtualizacaoStartup {
        if (whatsapp != null) whatsapp = whatsapp.isBlank() ? "" : Whatsapp.normalizar(whatsapp);
    }
}
