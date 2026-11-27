package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.DadosIniciais;
import com.ponti.api.model.AcaoAuditoria;
import com.ponti.api.model.RegistroAuditoria;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.RegistroAuditoriaRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.LoginService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Perfil de administrador de ponta a ponta, com tokens JWT de verdade:
// conta criada pelas variáveis de ambiente, acesso só do admin à área /admin,
// lista de usuários, bloqueio/desbloqueio e registro de auditoria.
@SpringBootTest(properties = {
        "ponti.admin.email=Admin@Teste.com",
        "ponti.admin.senha=senha-de-teste-do-admin"
})
@AutoConfigureMockMvc
@Transactional
class AdminTest {

    static final String EMAIL_ADMIN = "admin@teste.com";
    static final String SENHA_ADMIN = "senha-de-teste-do-admin";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RegistroAuditoriaRepository auditoriaRepository;

    String tokenAdmin;
    String tokenPetcare;
    Usuario juliana;

    @BeforeEach
    void entrar() throws Exception {
        tokenAdmin = token(EMAIL_ADMIN, SENHA_ADMIN);
        tokenPetcare = token("petcare@exemplo.com", DadosIniciais.SENHA_EXEMPLO);
        juliana = usuarioRepository.findByEmail("juliana@exemplo.com").orElseThrow();
    }

    String token(String email, String senha) throws Exception {
        String json = login(email, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("token").asText();
    }

    ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))));
    }

    ResultActions como(String token, MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header(HttpHeaders.AUTHORIZATION, token));
    }

    ResultActions como(String token, MockHttpServletRequestBuilder req, Object corpo) throws Exception {
        return mockMvc.perform(req.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    ResultActions bloquear(Long usuarioId, String motivo) throws Exception {
        return como(tokenAdmin, post("/admin/usuarios/" + usuarioId + "/bloquear"), Map.of("motivo", motivo));
    }

    @Nested
    class ContaDoAdmin {

        @Test
        void entraComOEmailEASenhaDasVariaveisDeAmbiente() throws Exception {
            login(EMAIL_ADMIN, SENHA_ADMIN)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.tipo").value("ADMIN"))
                    .andExpect(jsonPath("$.usuario.nome").value("Administrador"))
                    .andExpect(jsonPath("$.usuario.startupId").doesNotExist())
                    .andExpect(jsonPath("$.usuario.mentorId").doesNotExist());
        }

        @Test
        void senhaGuardadaComoHashENuncaEmTextoPuro() {
            Usuario admin = usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow();
            assertThat(admin.getTipo()).isEqualTo(TipoUsuario.ADMIN);
            assertThat(admin.getSenhaHash()).isNotEqualTo(SENHA_ADMIN).startsWith("$2");
        }

        @Test
        void senhaErradaDa401() throws Exception {
            login(EMAIL_ADMIN, "senha-errada-123").andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class QuemAcessaOQue {

        @Test
        void semLoginDa401() throws Exception {
            mockMvc.perform(get("/admin/usuarios")).andExpect(status().isUnauthorized());
        }

        @Test
        void startupEMentorRecebem403NaAreaDoAdmin() throws Exception {
            como(tokenPetcare, get("/admin/usuarios")).andExpect(status().isForbidden());
            como(token("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO), get("/admin/auditoria"))
                    .andExpect(status().isForbidden());
            como(tokenPetcare, post("/admin/usuarios/" + juliana.getId() + "/bloquear"), Map.of("motivo", "teste"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminNaoUsaAsRotasDeStartupEMentor() throws Exception {
            como(tokenAdmin, get("/mentores")).andExpect(status().isForbidden());
            como(tokenAdmin, get("/acompanhamentos")).andExpect(status().isForbidden());
        }

        @Test
        void meDoAdminFunciona() throws Exception {
            como(tokenAdmin, get("/auth/me")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("ADMIN"));
        }
    }

    @Nested
    class ListaDeUsuarios {

        @Test
        void listaTodosSemSenhaNemWhatsapp() throws Exception {
            String json = como(tokenAdmin, get("/admin/usuarios").param("tamanho", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[*].email", hasItem("juliana@exemplo.com")))
                    .andReturn().getResponse().getContentAsString();
            assertThat(json).doesNotContain("senha").doesNotContain("whatsapp").doesNotContain("$2a$");
            assertThat(objectMapper.readTree(json).get("total").asLong()).isEqualTo(usuarioRepository.count());
        }

        @Test
        void buscaPorNomeOuEmail() throws Exception {
            como(tokenAdmin, get("/admin/usuarios").param("busca", "JULIANA"))
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.itens[0].email").value("juliana@exemplo.com"))
                    .andExpect(jsonPath("$.itens[0].nome").value("Juliana G."))
                    .andExpect(jsonPath("$.itens[0].tipo").value("MENTOR"));
            como(tokenAdmin, get("/admin/usuarios").param("busca", "PetCare"))
                    .andExpect(jsonPath("$.itens[0].email").value("petcare@exemplo.com"));
        }

        @Test
        void filtraPorTipoESituacao() throws Exception {
            como(tokenAdmin, get("/admin/usuarios").param("tipo", "STARTUP").param("tamanho", "100"))
                    .andExpect(jsonPath("$.itens[*].tipo", everyItem(org.hamcrest.Matchers.is("STARTUP"))));
            bloquear(juliana.getId(), "Teste do filtro").andExpect(status().isOk());
            como(tokenAdmin, get("/admin/usuarios").param("situacao", "BLOQUEADO"))
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.itens[0].email").value("juliana@exemplo.com"))
                    .andExpect(jsonPath("$.itens[0].motivoBloqueio").value("Teste do filtro"));
        }

        @Test
        void paginacao() throws Exception {
            como(tokenAdmin, get("/admin/usuarios").param("pagina", "1").param("tamanho", "2"))
                    .andExpect(jsonPath("$.itens.length()").value(2))
                    .andExpect(jsonPath("$.pagina").value(1))
                    .andExpect(jsonPath("$.tamanho").value(2));
        }
    }

    @Nested
    class Bloqueio {

        @Test
        void contaBloqueadaNaoEntraERecebeMensagemClara() throws Exception {
            bloquear(juliana.getId(), "Avaliações com reclamações repetidas")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bloqueado").value(true));

            login("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(LoginService.CONTA_BLOQUEADA));
        }

        @Test
        void senhaErradaDeContaBloqueadaContinuaDando401() throws Exception {
            bloquear(juliana.getId(), "Teste").andExpect(status().isOk());
            // Não revela o bloqueio para quem não sabe a senha
            login("juliana@exemplo.com", "senha-errada-123").andExpect(status().isUnauthorized());
        }

        @Test
        void tokenQueJaExistiaParaDeValer() throws Exception {
            String tokenJuliana = token("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO);
            como(tokenJuliana, get("/acompanhamentos")).andExpect(status().isOk());

            bloquear(juliana.getId(), "Teste").andExpect(status().isOk());

            como(tokenJuliana, get("/acompanhamentos")).andExpect(status().isUnauthorized());
            como(tokenJuliana, get("/auth/me")).andExpect(status().isForbidden());
        }

        @Test
        void mentorBloqueadoSomeDaListaEDasRecomendacoes() throws Exception {
            String id = String.valueOf(juliana.getMentorId());
            como(tokenPetcare, get("/mentores")).andExpect(jsonPath("$[*].id", hasItem(Integer.valueOf(id))));

            bloquear(juliana.getId(), "Teste").andExpect(status().isOk());

            como(tokenPetcare, get("/mentores"))
                    .andExpect(jsonPath("$[*].id", not(hasItem(Integer.valueOf(id)))));
            como(tokenPetcare, get("/mentores/recomendados"))
                    .andExpect(jsonPath("$[*].mentor.id", not(hasItem(Integer.valueOf(id)))));
        }

        @Test
        void startupBloqueadaSomeDaListaEDasRecomendacoesDoMentor() throws Exception {
            Usuario petcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow();
            String tokenJuliana = token("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO);

            bloquear(petcare.getId(), "Teste").andExpect(status().isOk());

            Integer id = petcare.getStartupId().intValue();
            como(tokenJuliana, get("/startups")).andExpect(jsonPath("$[*].id", not(hasItem(id))));
            como(tokenJuliana, get("/startups/recomendadas"))
                    .andExpect(jsonPath("$[*].startup.id", not(hasItem(id))));
        }

        @Test
        void motivoEObrigatorio() throws Exception {
            bloquear(juliana.getId(), "  ").andExpect(status().isBadRequest());
            assertThat(usuarioRepository.findById(juliana.getId()).orElseThrow().estaBloqueado()).isFalse();
        }

        @Test
        void naoBloqueiaAdministradorNemBloqueiaDuasVezes() throws Exception {
            Usuario admin = usuarioRepository.findByEmail(EMAIL_ADMIN).orElseThrow();
            bloquear(admin.getId(), "Teste").andExpect(status().isConflict());

            bloquear(juliana.getId(), "Teste").andExpect(status().isOk());
            bloquear(juliana.getId(), "Teste").andExpect(status().isConflict());
        }

        @Test
        void usuarioInexistenteDa404() throws Exception {
            bloquear(999_999L, "Teste").andExpect(status().isNotFound());
        }

        @Test
        void desbloquearDevolveOAcesso() throws Exception {
            bloquear(juliana.getId(), "Teste").andExpect(status().isOk());
            como(tokenAdmin, post("/admin/usuarios/" + juliana.getId() + "/desbloquear"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bloqueado").value(false))
                    .andExpect(jsonPath("$.motivoBloqueio").doesNotExist());
            login("juliana@exemplo.com", DadosIniciais.SENHA_EXEMPLO).andExpect(status().isOk());
        }

        @Test
        void desbloquearQuemNaoEstaBloqueadoDa409() throws Exception {
            como(tokenAdmin, post("/admin/usuarios/" + juliana.getId() + "/desbloquear"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class RegistroDeAuditoria {

        @Test
        void bloqueioEDesbloqueioFicamRegistradosComAutorMotivoEData() throws Exception {
            bloquear(juliana.getId(), "Reclamações repetidas").andExpect(status().isOk());
            como(tokenAdmin, post("/admin/usuarios/" + juliana.getId() + "/desbloquear"),
                    Map.of("motivo", "Situação resolvida")).andExpect(status().isOk());

            List<RegistroAuditoria> registros =
                    auditoriaRepository.findByAlvoUsuarioIdOrderByCriadoEmDescIdDesc(juliana.getId());
            assertThat(registros).hasSize(2);
            assertThat(registros).extracting(RegistroAuditoria::getAcao)
                    .containsExactly(AcaoAuditoria.DESBLOQUEAR_USUARIO, AcaoAuditoria.BLOQUEAR_USUARIO);
            assertThat(registros).allSatisfy(r -> {
                assertThat(r.getAutorEmail()).isEqualTo(EMAIL_ADMIN);
                assertThat(r.getAlvoDescricao()).isEqualTo("Juliana G. (juliana@exemplo.com)");
                assertThat(r.getCriadoEm()).isNotNull();
            });
            assertThat(registros.get(1).getMotivo()).isEqualTo("Reclamações repetidas");
            assertThat(registros.get(0).getMotivo()).isEqualTo("Situação resolvida");
        }

        @Test
        void adminVeORegistroMaisRecentePrimeiro() throws Exception {
            bloquear(juliana.getId(), "Primeiro").andExpect(status().isOk());
            como(tokenAdmin, post("/admin/usuarios/" + juliana.getId() + "/desbloquear")).andExpect(status().isOk());

            como(tokenAdmin, get("/admin/auditoria"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[0].acao").value("DESBLOQUEAR_USUARIO"))
                    .andExpect(jsonPath("$.itens[1].acao").value("BLOQUEAR_USUARIO"))
                    .andExpect(jsonPath("$.itens[1].motivo").value("Primeiro"));
        }

        @Test
        void tentativaRecusadaNaoGeraRegistro() throws Exception {
            long antes = auditoriaRepository.count();
            bloquear(juliana.getId(), "").andExpect(status().isBadRequest());
            como(tokenPetcare, post("/admin/usuarios/" + juliana.getId() + "/bloquear"), Map.of("motivo", "x"));
            assertThat(auditoriaRepository.count()).isEqualTo(antes);
        }
    }
}
