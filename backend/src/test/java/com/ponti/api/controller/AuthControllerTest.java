package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// TESTE DE INTEGRAÇÃO: sobe a aplicação inteira (Spring + banco H2 em memória) e faz
// requisições HTTP de mentira com o MockMvc, passando por controller -> service -> banco.
// Mais lento que o teste de unidade, mas prova que as peças funcionam juntas.
//
// @Transactional: cada teste roda numa transação que é desfeita no final,
// então um teste nunca enxerga o que outro gravou (todos começam do mesmo banco).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // ---------- Dados de exemplo válidos. Cada teste parte deles e muda só o que interessa. ----------

    static Map<String, Object> startupValida() {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "AgroTech Recife");
        body.put("areaAtuacao", "Agritech");
        body.put("sobre", "Monitoramento de lavouras com sensores de baixo custo.");
        body.put("email", "contato@agrotech.com");
        body.put("senha", "senhaSegura123");
        return body;
    }

    static Map<String, Object> mentorValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Maria Souza");
        body.put("areaAtuacao", "Produto, Validação de MVP");
        body.put("anosExperiencia", 10);
        body.put("sobre", "Ex-fundadora com 10 anos de experiência em startups.");
        body.put("email", "maria@email.com");
        body.put("senha", "senhaSegura123");
        return body;
    }

    // Converte o Map em JSON e faz o POST, como o front faria
    ResultActions enviar(String rota, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(rota)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // =====================================================================================
    @Nested
    class CadastroDeStartup {

        @Test
        void criaUsuarioEStartupERetorna201() throws Exception {
            // Verifica a RESPOSTA HTTP: status e campos do JSON
            enviar("/auth/cadastro/startup", startupValida())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("contato@agrotech.com"))
                    .andExpect(jsonPath("$.tipo").value("STARTUP"))
                    .andExpect(jsonPath("$.startupId").isNumber())
                    .andExpect(jsonPath("$.mentorId").value(nullValue()))
                    .andExpect(jsonPath("$.nome").value("AgroTech Recife"));

            // Verifica o BANCO: cada campo do formulário foi gravado no lugar certo
            Usuario usuario = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow();
            Startup startup = startupRepository.findById(usuario.getStartupId()).orElseThrow();

            assertThat(usuario.getTipo()).isEqualTo(TipoUsuario.STARTUP);
            assertThat(startup.getNome()).isEqualTo("AgroTech Recife");
            assertThat(startup.getSegmento()).isEqualTo("Agritech");
            assertThat(startup.getDescricao()).isEqualTo("Monitoramento de lavouras com sensores de baixo custo.");
        }

        @Test
        void naoDefineNivelDeMaturidadeMesmoSeEnviado() throws Exception {
            // A maturidade vem de outro formulário. Mesmo que alguém mande o campo, ele é ignorado.
            Map<String, Object> body = startupValida();
            body.put("estagioMaturidade", 5);

            enviar("/auth/cadastro/startup", body).andExpect(status().isCreated());

            Usuario usuario = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow();
            Startup startup = startupRepository.findById(usuario.getStartupId()).orElseThrow();
            assertThat(startup.getEstagioMaturidade()).isNull();
        }

        // @ParameterizedTest roda o MESMO teste várias vezes, uma para cada linha de dadosInvalidos().
        // O nome entre {0} aparece no relatório, então dá para saber qual caso falhou.
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.ponti.api.controller.AuthControllerTest#dadosInvalidosStartup")
        void rejeitaDadosInvalidosCom400ENaoGravaNada(String caso, String campo, Object valor) throws Exception {
            long usuariosAntes = usuarioRepository.count();
            long startupsAntes = startupRepository.count();
            Map<String, Object> body = startupValida();
            body.put(campo, valor);

            enviar("/auth/cadastro/startup", body).andExpect(status().isBadRequest());

            assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
            assertThat(startupRepository.count()).isEqualTo(startupsAntes);
        }
    }

    static Stream<Arguments> dadosInvalidosStartup() {
        return Stream.of(
                Arguments.of("sem nome", "nome", null),
                Arguments.of("nome em branco", "nome", "   "),
                Arguments.of("nome com mais de 255 caracteres", "nome", "a".repeat(256)),
                Arguments.of("sem área de atuação", "areaAtuacao", null),
                Arguments.of("sem sobre", "sobre", null),
                Arguments.of("sobre com mais de 2000 caracteres", "sobre", "a".repeat(2001)),
                Arguments.of("sem e-mail", "email", null),
                Arguments.of("e-mail inválido", "email", "nao-e-um-email"),
                Arguments.of("sem senha", "senha", null),
                Arguments.of("senha com menos de 8 caracteres", "senha", "1234567"),
                Arguments.of("senha com mais de 72 caracteres", "senha", "a".repeat(73)));
    }

    // =====================================================================================
    @Nested
    class CadastroDeMentor {

        @Test
        void criaUsuarioEMentorERetorna201() throws Exception {
            enviar("/auth/cadastro/mentor", mentorValido())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("maria@email.com"))
                    .andExpect(jsonPath("$.tipo").value("MENTOR"))
                    .andExpect(jsonPath("$.mentorId").isNumber())
                    .andExpect(jsonPath("$.startupId").value(nullValue()))
                    .andExpect(jsonPath("$.nome").value("Maria Souza"));

            Usuario usuario = usuarioRepository.findByEmail("maria@email.com").orElseThrow();
            Mentor mentor = mentorRepository.findById(usuario.getMentorId()).orElseThrow();

            assertThat(usuario.getTipo()).isEqualTo(TipoUsuario.MENTOR);
            assertThat(mentor.getNome()).isEqualTo("Maria Souza");
            assertThat(mentor.getAreasAtuacao()).isEqualTo("Produto, Validação de MVP");
            assertThat(mentor.getAnosExperiencia()).isEqualTo(10);
            assertThat(mentor.getBio()).isEqualTo("Ex-fundadora com 10 anos de experiência em startups.");
        }

        @Test
        void aceitaZeroAnosDeExperiencia() throws Exception {
            // Teste de LIMITE: 0 é o menor valor permitido, então precisa passar
            Map<String, Object> body = mentorValido();
            body.put("anosExperiencia", 0);

            enviar("/auth/cadastro/mentor", body).andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.ponti.api.controller.AuthControllerTest#dadosInvalidosMentor")
        void rejeitaDadosInvalidosCom400ENaoGravaNada(String caso, String campo, Object valor) throws Exception {
            long usuariosAntes = usuarioRepository.count();
            long mentoresAntes = mentorRepository.count();
            Map<String, Object> body = mentorValido();
            body.put(campo, valor);

            enviar("/auth/cadastro/mentor", body).andExpect(status().isBadRequest());

            assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
            assertThat(mentorRepository.count()).isEqualTo(mentoresAntes);
        }
    }

    static Stream<Arguments> dadosInvalidosMentor() {
        return Stream.of(
                Arguments.of("sem nome", "nome", null),
                Arguments.of("sem área de atuação", "areaAtuacao", null),
                Arguments.of("sem anos de experiência", "anosExperiencia", null),
                Arguments.of("anos de experiência negativo", "anosExperiencia", -1),
                Arguments.of("anos de experiência acima de 70", "anosExperiencia", 71),
                Arguments.of("anos de experiência não numérico", "anosExperiencia", "dez"),
                Arguments.of("sem sobre", "sobre", null),
                Arguments.of("e-mail inválido", "email", "nao-e-um-email"),
                Arguments.of("senha com menos de 8 caracteres", "senha", "1234567"));
    }

    // =====================================================================================
    // Regras que valem para os dois tipos de usuário
    @Nested
    class RegrasDeEmailESenha {

        @Test
        void guardaEmailSemEspacosEEmMinusculas() throws Exception {
            Map<String, Object> body = startupValida();
            body.put("email", "  Contato@AgroTech.COM ");

            enviar("/auth/cadastro/startup", body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("contato@agrotech.com"));
        }

        @Test
        void recusaEmailJaCadastradoCom409MesmoEntreTiposDiferentes() throws Exception {
            enviar("/auth/cadastro/startup", startupValida()).andExpect(status().isCreated());
            long usuariosAntes = usuarioRepository.count();
            long mentoresAntes = mentorRepository.count();

            // Mesmo e-mail, com maiúsculas, tentando virar mentor
            Map<String, Object> mentor = mentorValido();
            mentor.put("email", "CONTATO@agrotech.com");

            // No servidor real a mensagem vai no JSON de erro ("message"). O MockMvc não passa pela
            // rota /error do Spring Boot que monta esse JSON, então aqui ela fica no "reason" da resposta.
            enviar("/auth/cadastro/mentor", mentor)
                    .andExpect(status().isConflict())
                    .andExpect(status().reason("Este e-mail já está cadastrado"));

            // O @Transactional do service garante que não sobrou um mentor "órfão" (sem usuário)
            assertThat(mentorRepository.count()).isEqualTo(mentoresAntes);
            assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
        }

        @Test
        void guardaSomenteOHashBCryptDaSenha() throws Exception {
            enviar("/auth/cadastro/startup", startupValida()).andExpect(status().isCreated());

            String hash = usuarioRepository.findByEmail("contato@agrotech.com").orElseThrow().getSenhaHash();

            assertThat(hash).isNotEqualTo("senhaSegura123");  // não é a senha em texto puro
            assertThat(hash).startsWith("$2a$");                // é um hash no formato BCrypt
            assertThat(passwordEncoder.matches("senhaSegura123", hash)).isTrue();   // a senha certa confere
            assertThat(passwordEncoder.matches("senhaErrada123", hash)).isFalse();  // a errada não
        }

        @Test
        void respostaNaoExpoeSenhaNemHash() throws Exception {
            enviar("/auth/cadastro/mentor", mentorValido())
                    .andExpect(status().isCreated())
                    .andExpect(content().string(not(containsString("senha"))))
                    .andExpect(content().string(not(containsString("$2a$"))));
        }
    }
}
