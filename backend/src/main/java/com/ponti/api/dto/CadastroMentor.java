package com.ponti.api.dto;

import com.ponti.api.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

// Corpo de POST /auth/cadastro/mentor. Só os campos do formulário de cadastro.
// A senha tem no máximo 72 caracteres porque o BCrypt ignora o que passa disso.
public record CadastroMentor(
        // Etapa 1: conta
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String senha,

        // Etapa 2: perfil profissional
        @NotBlank @Size(max = 255) String areaAtuacao,
        @Size(max = 255) String ecossistemas,
        @NotNull @Min(0) @Max(70) Integer anosExperiencia,
        @NotBlank @Size(max = 2000) String sobre,
        @Size(max = 500) String fotoUrl,
        @Size(max = 500) String videoUrl,
        @Size(max = 500) String linkedinUrl,
        @Valid @Size(max = 20) List<NovaCertificacao> certificacoes,

        // Etapa 3: como é a mentoria. Valor 0 = mentoria gratuita.
        @NotNull @DecimalMin("0") @DecimalMax("100000") @Digits(integer = 6, fraction = 2) BigDecimal valorSessao,
        @NotNull @Min(15) @Max(240) Integer duracaoSessaoMinutos) {

    // Normaliza antes da validação, para " Ana@Email.com " ser aceito e não duplicar "ana@email.com"
    public CadastroMentor {
        email = Usuario.normalizarEmail(email);
    }
}
