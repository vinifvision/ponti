package com.ponti.api.dto;

import com.ponti.api.model.Usuario;
import jakarta.validation.constraints.NotBlank;

// Corpo de POST /auth/login.
// Sem @Email nem tamanho mínimo de senha: no login só importa se confere com o cadastro.
public record CredenciaisLogin(
        @NotBlank String email,
        @NotBlank String senha) {

    public CredenciaisLogin {
        email = Usuario.normalizarEmail(email);
    }
}
