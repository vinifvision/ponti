package com.ponti.api.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.seguranca.UsuarioLogado;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Nos testes, faz a requisição "como se" um usuário estivesse logado, sem precisar gerar token:
//   mockMvc.perform(get("/mentores").with(Login.comoStartup(1L)))
// O fluxo real com token JWT é testado em SegurancaTest e LoginTest.
public final class Login {

    private Login() {
    }

    public static RequestPostProcessor comoStartup(Long startupId) {
        return como(new UsuarioLogado(null, TipoUsuario.STARTUP, startupId, null));
    }

    public static RequestPostProcessor comoMentor(Long mentorId) {
        return como(new UsuarioLogado(null, TipoUsuario.MENTOR, null, mentorId));
    }

    private static RequestPostProcessor como(UsuarioLogado logado) {
        return authentication(new UsernamePasswordAuthenticationToken(
                logado, null, List.of(new SimpleGrantedAuthority("ROLE_" + logado.tipo().name()))));
    }
}
