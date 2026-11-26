package com.ponti.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.support.Login;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// Com os dados de demonstração ligados, a aba de acompanhamentos já sobe com exemplos nas contas
// de demonstração (para o pitch). Banco separado, para não aparecer nos outros testes.
@SpringBootTest(properties = {"ponti.demo.completo=true", "spring.datasource.url=jdbc:h2:mem:ponti-demo-acompanhamentos"})
@AutoConfigureMockMvc
class DadosDemonstracaoAcompanhamentosTest {

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AcompanhamentoRepository acompanhamentoRepository;
    @Autowired DadosDemonstracaoAcompanhamentos dados;

    @Test
    void petcareTemPropostaAtivoEEncerrado() throws Exception {
        Long petcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow().getStartupId();

        mockMvc.perform(get("/acompanhamentos").with(Login.comoStartup(petcare)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.status == 'PROPOSTO')].parceiro.nome").value("Patrícia Nunes"))
                .andExpect(jsonPath("$[?(@.status == 'ATIVO')].parceiro.nome").value("Juliana G."))
                .andExpect(jsonPath("$[?(@.status == 'ATIVO')].atividadesTotal").value(5))
                .andExpect(jsonPath("$[?(@.status == 'ATIVO')].atividadesAtrasadas").value(2)) // devolvida e pendente, ambas vencidas
                .andExpect(jsonPath("$[?(@.status == 'ENCERRADO')].atividadesAprovadas").value(3));

        mockMvc.perform(get("/acompanhamentos/painel").with(Login.comoStartup(petcare)))
                .andExpect(jsonPath("$.acompanhamentosAtivos").value(1))
                .andExpect(jsonPath("$.metasCumpridas").value(2))
                .andExpect(jsonPath("$.metasTotal").value(4))
                .andExpect(jsonPath("$.entregasNoPrazo").value(100));
    }

    @Test
    void julianaTemDoisAtivosEUmaPropostaEsperando() throws Exception {
        Long juliana = usuarioRepository.findByEmail("juliana@exemplo.com").orElseThrow().getMentorId();

        mockMvc.perform(get("/acompanhamentos").with(Login.comoMentor(juliana)))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.parceiro.nome == 'Marés Limpas')].pendencias").value(1))
                .andExpect(jsonPath("$[?(@.parceiro.nome == 'SaúdeAgora')].status").value("PROPOSTO"));
    }

    @Test
    void naoDuplicaAoReiniciar() {
        long antes = acompanhamentoRepository.count();
        dados.run();
        assertThat(acompanhamentoRepository.count()).isEqualTo(antes);
    }
}
