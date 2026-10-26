package com.ponti.api.dto;

import com.ponti.api.model.Certificacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dados de uma certificação enviados pelo front (no cadastro ou depois, no perfil)
public record NovaCertificacao(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 255) String instituicao,
        @Min(1950) @Max(2100) Integer anoInicio,
        @Min(1950) @Max(2100) Integer anoFim,
        @Size(max = 1000) String descricao) {

    // O ano de término não pode vir antes do ano de início
    @AssertTrue(message = "O ano de término não pode ser anterior ao ano de início")
    public boolean isPeriodoValido() {
        return anoInicio == null || anoFim == null || anoFim >= anoInicio;
    }

    public Certificacao paraEntidade() {
        Certificacao c = new Certificacao();
        c.setTitulo(titulo.trim());
        c.setInstituicao(instituicao);
        c.setAnoInicio(anoInicio);
        c.setAnoFim(anoFim);
        c.setDescricao(descricao);
        return c;
    }
}
