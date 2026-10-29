package com.ponti.api.dto;

import com.ponti.api.model.Tema;
import com.ponti.api.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

// Corpo de POST /auth/cadastro/startup. Só os campos do formulário de cadastro:
// o nível de maturidade é definido depois, em outro formulário.
// A senha tem no máximo 72 caracteres porque o BCrypt ignora o que passa disso.
public record CadastroStartup(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Size(max = 255) String areaAtuacao,
        @NotBlank @Size(max = 2000) String sobre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String senha,

        // Opcionais: completam o perfil público da startup
        @Size(max = 255) String cidade,
        @Size(max = 2) String estado,
        @Size(max = 500) String videoPitchUrl,
        @Size(max = 500) String fotoUrl,
        @Size(max = 500) String site,
        // Temas em que a startup precisa de ajuda (até 3); alimentam o match com os mentores
        @Size(max = 3) Set<Tema> necessidades) {

    // Normaliza antes da validação, para " Ana@Email.com " ser aceito e não duplicar "ana@email.com"
    public CadastroStartup {
        email = Usuario.normalizarEmail(email);
    }
}
