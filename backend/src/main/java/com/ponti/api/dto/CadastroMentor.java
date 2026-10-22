package com.ponti.api.dto;

import com.ponti.api.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corpo de POST /auth/cadastro/mentor. Só os campos do formulário de cadastro.
// A senha tem no máximo 72 caracteres porque o BCrypt ignora o que passa disso.
public record CadastroMentor(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Size(max = 255) String areaAtuacao,
        @NotNull @Min(0) @Max(70) Integer anosExperiencia,
        @NotBlank @Size(max = 2000) String sobre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String senha) {

    // Normaliza antes da validação, para " Ana@Email.com " ser aceito e não duplicar "ana@email.com"
    public CadastroMentor {
        email = Usuario.normalizarEmail(email);
    }
}
