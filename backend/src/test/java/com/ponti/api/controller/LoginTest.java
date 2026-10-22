package com.ponti.api.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.ponti.api.config.DadosIniciais;
import com.ponti.api.repository.UsuarioRepository;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// TESTE DE INTEGRAÇÃO do login (POST /auth/login) e de "quem sou eu" (GET /auth/me).
// Usa o usuário de exemplo criado pelo DadosIniciais: petcare@exemplo.com / ponti1234.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginTest {

    static final String EMAIL = "petcare@exemplo.com";
    static final String SENHA = DadosIniciais.SENHA_EXEMPLO;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;

    ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))));
    }

    // Faz login e devolve só o token (lido do JSON da resposta)
    String tokenValido() throws Exception {
        String json = login(EMAIL, SENHA).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    ResultActions eu(String authorization) throws Exception {
        var requisicao = get("/auth/me");
        if (authorization != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(requisicao);
    }

    // =====================================================================================
    @Nested
    class Login {

        @Test
        void comEmailESenhaCorretosDevolveTokenEUsuario() throws Exception {
            login(EMAIL, SENHA)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                    .andExpect(jsonPath("$.expiraEm").isString())
                    .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                    .andExpect(jsonPath("$.usuario.tipo").value("STARTUP"))
                    .andExpect(jsonPath("$.usuario.startupId").isNumber())
                    .andExpect(jsonPath("$.usuario.nome").value("PetCare Digital"));
        }

        @Test
        void funcionaComUsuarioRecemCadastrado() throws Exception {
            // Fluxo completo: cadastra pela API e em seguida faz login com a mesma senha
            mockMvc.perform(post("/auth/cadastro/mentor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nome", "Maria Souza",
                                    "areaAtuacao", "Produto",
                                    "anosExperiencia", 10,
                                    "sobre", "Mentora de produto.",
                                    "email", "maria@email.com",
                                    "senha", "senhaSegura123"))))
                    .andExpect(status().isCreated());

            login("maria@email.com", "senhaSegura123")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.tipo").value("MENTOR"));
        }

        @Test
        void aceitaEmailComMaiusculasEEspacos() throws Exception {
            login("  PetCare@Exemplo.COM ", SENHA).andExpect(status().isOk());
        }

        @Test
        void senhaErradaDa401() throws Exception {
            login(EMAIL, "senhaErrada")
                    .andExpect(status().isUnauthorized())
                    .andExpect(status().reason("E-mail ou senha inválidos"));
        }

        @Test
        void senhaComMaiusculasDiferentesDa401() throws Exception {
            // Diferente do e-mail, a senha diferencia maiúsculas de minúsculas
            login(EMAIL, SENHA.toUpperCase()).andExpect(status().isUnauthorized());
        }

        @Test
        void emailInexistenteDaAMesmaRespostaQueSenhaErrada() throws Exception {
            // Mesma mensagem dos dois casos: quem tenta adivinhar não descobre se o e-mail existe
            login("ninguem@exemplo.com", SENHA)
                    .andExpect(status().isUnauthorized())
                    .andExpect(status().reason("E-mail ou senha inválidos"));
        }

        @Test
        void camposEmBrancoDao400() throws Exception {
            login("", SENHA).andExpect(status().isBadRequest());
            login(EMAIL, "").andExpect(status().isBadRequest());
        }

        @Test
        void respostaNaoExpoeSenhaNemHash() throws Exception {
            login(EMAIL, SENHA)
                    .andExpect(content().string(not(containsString(SENHA))))
                    .andExpect(content().string(not(containsString("$2a$"))));
        }
    }

    // =====================================================================================
    @Nested
    class QuemSouEu {

        @Test
        void comTokenValidoDevolveOUsuarioLogado() throws Exception {
            eu("Bearer " + tokenValido())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.tipo").value("STARTUP"))
                    .andExpect(jsonPath("$.nome").value("PetCare Digital"));
        }

        @Test
        void semTokenDa401() throws Exception {
            eu(null).andExpect(status().isUnauthorized());
        }

        @Test
        void semPrefixoBearerDa401() throws Exception {
            eu(tokenValido()).andExpect(status().isUnauthorized());
        }

        @Test
        void comTokenInvalidoDa401() throws Exception {
            eu("Bearer isso-nao-e-um-token")
                    .andExpect(status().isUnauthorized())
                    .andExpect(status().reason("Token inválido ou expirado"));
        }

        @Test
        void tokenDeUsuarioApagadoDa401() throws Exception {
            // O token continua bem assinado, mas o usuário não existe mais no banco
            String token = tokenValido();
            usuarioRepository.delete(usuarioRepository.findByEmail(EMAIL).orElseThrow());

            eu("Bearer " + token).andExpect(status().isUnauthorized());
        }
    }
}
