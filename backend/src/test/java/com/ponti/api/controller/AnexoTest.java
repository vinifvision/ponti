package com.ponti.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AnexoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.support.Login;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Acompanhamentos, parte 4: arquivos anexados nas atividades (mentor) e nas entregas (startup).
// Nos testes o armazenamento é o local (em memória); em produção é o Supabase Storage,
// testado à parte em ArmazenamentoSupabaseTest.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnexoTest {

    static final byte[] PDF = "%PDF-1.4 conteudo de teste".getBytes();
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MentorRepository mentorRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired AcompanhamentoRepository acompanhamentoRepository;
    @Autowired AnexoRepository anexoRepository;

    Long mentorId;
    Long startupId;
    Long acompanhamentoId;
    RequestPostProcessor mentor;
    RequestPostProcessor startup;

    @BeforeEach
    void preparar() {
        mentorId = novoMentor();
        startupId = novaStartup("Startup dos Anexos");
        acompanhamentoId = novoAcompanhamento();
        mentor = Login.comoMentor(mentorId);
        startup = Login.comoStartup(startupId);
    }

    // ---------------------------------------------------------------- envio e download

    @Test
    void mentorCriaAtividadeComMateriais() throws Exception {
        criarAtividade(arquivo("Modelo de roteiro.pdf", PDF), arquivo("mapa.png", PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Atividade com material"))
                .andExpect(jsonPath("$.prazo").value(prazo().toString() + ":00"))
                .andExpect(jsonPath("$.anexos.length()").value(2))
                .andExpect(jsonPath("$.anexos[0].nome").value("Modelo de roteiro.pdf"))
                .andExpect(jsonPath("$.anexos[0].tamanho").value(PDF.length))
                .andExpect(jsonPath("$.anexos[1].nome").value("mapa.png"));
    }

    @Test
    void linkBaixaOArquivoSemLoginEComNomeOriginal() throws Exception {
        JsonNode atividade = json(criarAtividade(arquivo("Modelo de roteiro.pdf", PDF)).andExpect(status().isCreated()));
        String link = atividade.get("anexos").get(0).get("url").asText();
        assertThat(link).startsWith("http://localhost/arquivos-locais?");

        byte[] baixado = mockMvc.perform(get(URI.create(link)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Modelo")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(baixado).isEqualTo(PDF);
    }

    @Test
    void linkAdulteradoNaoBaixa() throws Exception {
        JsonNode atividade = json(criarAtividade(arquivo("a.pdf", PDF)));
        String link = atividade.get("anexos").get(0).get("url").asText();
        // Trocar o caminho (tentar pegar outro arquivo) invalida a assinatura
        String adulterado = link.replaceFirst("caminho=[^&]+", "caminho=acompanhamentos%2F1%2Foutro.pdf");
        mockMvc.perform(get(URI.create(adulterado))).andExpect(status().isNotFound());
        // Esticar a validade também invalida
        String esticado = link.replaceFirst("expira=\\d+", "expira=99999999999");
        mockMvc.perform(get(URI.create(esticado))).andExpect(status().isNotFound());
    }

    @Test
    void startupEntregaSoComArquivo() throws Exception {
        long atividade = json(criarAtividade()).get("id").asLong();
        MockMultipartHttpServletRequestBuilder req = multipart("/atividades/" + atividade + "/entrega");
        req.file(arquivo("Relatorio.pdf", PDF));
        mockMvc.perform(req.with(startup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.entregas[0].texto").value(""))
                .andExpect(jsonPath("$.entregas[0].anexos[0].nome").value("Relatorio.pdf"))
                .andExpect(jsonPath("$.entregas[0].anexos[0].url").exists());
    }

    @Test
    void entregaComTextoLinkEArquivo() throws Exception {
        long atividade = json(criarAtividade()).get("id").asLong();
        MockMultipartHttpServletRequestBuilder req = multipart("/atividades/" + atividade + "/entrega");
        req.file(arquivo("planilha.png", PNG)).param("texto", "Segue a planilha").param("link", "https://docs.google.com/x");
        mockMvc.perform(req.with(startup))
                .andExpect(jsonPath("$.entregas[0].texto").value("Segue a planilha"))
                .andExpect(jsonPath("$.entregas[0].link").value("https://docs.google.com/x"))
                .andExpect(jsonPath("$.entregas[0].anexos.length()").value(1));
    }

    // ---------------------------------------------------------------- regras dos arquivos

    @Test
    void recusaExtensaoNaoAceita() throws Exception {
        criarAtividade(arquivo("programa.exe", new byte[] {0x4D, 0x5A, 1}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("\"programa.exe\" não é um tipo aceito.")));
    }

    @Test
    void recusaArquivoDisfarcado() throws Exception {
        // Um executável renomeado para .pdf
        criarAtividade(arquivo("contrato.pdf", new byte[] {0x4D, 0x5A, (byte) 0x90, 0}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O conteúdo de \"contrato.pdf\" não combina com a extensão do arquivo."));
    }

    @Test
    void recusaVazioGrandeDemaisOuArquivosDemais() throws Exception {
        criarAtividade(arquivo("vazio.txt", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("\"vazio.txt\" está vazio."));

        byte[] grande = Arrays.copyOf(PDF, 10 * 1024 * 1024 + 1);
        criarAtividade(arquivo("grande.pdf", grande))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("\"grande.pdf\" passa de 10 MB."));

        MockMultipartFile[] seis = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) seis[i] = arquivo("a" + i + ".pdf", PDF);
        criarAtividade(seis)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dá para anexar até 5 arquivos."));

        // Nenhuma ficha de anexo ficou salva
        assertThat(anexoRepository.findByAcompanhamentoId(acompanhamentoId)).isEmpty();
    }

    @Test
    void nomeComPastaFicaSoComONomeDoArquivo() throws Exception {
        criarAtividade(arquivo("C:\\Users\\fulano\\Documentos\\plano.pdf", PDF))
                .andExpect(jsonPath("$.anexos[0].nome").value("plano.pdf"));
    }

    @Test
    void startupNaoAnexaNaAtividadeDoMentor() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/acompanhamentos/" + acompanhamentoId + "/atividades");
        req.file(arquivo("a.pdf", PDF)).param("titulo", "X").param("prazo", prazo().toString());
        mockMvc.perform(req.with(startup)).andExpect(status().isForbidden());
        assertThat(anexoRepository.findByAcompanhamentoId(acompanhamentoId)).isEmpty();
    }

    @Test
    void desfazerEntregaApagaAFichaDoAnexo() throws Exception {
        long atividade = json(criarAtividade()).get("id").asLong();
        MockMultipartHttpServletRequestBuilder req = multipart("/atividades/" + atividade + "/entrega");
        req.file(arquivo("Relatorio.pdf", PDF));
        mockMvc.perform(req.with(startup)).andExpect(status().isOk());
        assertThat(anexoRepository.findByAcompanhamentoId(acompanhamentoId)).hasSize(1);

        mockMvc.perform(delete("/atividades/" + atividade + "/entrega").with(startup)).andExpect(status().isOk());
        assertThat(anexoRepository.findByAcompanhamentoId(acompanhamentoId)).isEmpty();
    }

    // ---------------------------------------------------------------- LGPD (com o banco confirmando de verdade)

    // Este teste não roda dentro da transação do teste: cada chamada confirma no banco, como na vida real.
    // Assim dá para ver o arquivo saindo do armazenamento depois que a exclusão da conta é confirmada.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void excluirAContaApagaOsArquivosDoArmazenamento() throws Exception {
        try {
            JsonNode atividade = json(criarAtividade(arquivo("material.pdf", PDF)).andExpect(status().isCreated()));
            URI link = URI.create(atividade.get("anexos").get(0).get("url").asText());
            mockMvc.perform(get(link)).andExpect(status().isOk());

            mockMvc.perform(delete("/startups/" + startupId).with(startup)).andExpect(status().isNoContent());

            mockMvc.perform(get(link)).andExpect(status().isNotFound());
            assertThat(anexoRepository.findByAcompanhamentoId(acompanhamentoId)).isEmpty();
        } finally {
            // Limpeza: este teste grava de verdade no banco de teste, então não pode deixar sobras
            // (outros testes contam os mentores cadastrados)
            if (startupRepository.existsById(startupId)) {
                mockMvc.perform(delete("/startups/" + startupId).with(startup));
            }
            mentorRepository.deleteById(mentorId);
        }
    }

    // ---------------------------------------------------------------- apoio

    LocalDateTime prazo() {
        return LocalDateTime.now(RelogioConfig.FUSO).plusDays(5).withHour(23).withMinute(59).withSecond(0).withNano(0);
    }

    ResultActions criarAtividade(MockMultipartFile... arquivos) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/acompanhamentos/" + acompanhamentoId + "/atividades");
        for (MockMultipartFile a : arquivos) req.file(a);
        // Igual ao front: o prazo vai como texto "2026-11-19T23:59:00"
        req.param("titulo", "Atividade com material").param("descricao", "Use o modelo.").param("prazo", prazo() + ":00");
        return mockMvc.perform(req.with(mentor));
    }

    static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivos", nome, "application/octet-stream", conteudo);
    }

    JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }

    Long novoAcompanhamento() {
        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentorId);
        a.setStartupId(startupId);
        a.setObjetivo("Objetivo do teste.");
        a.setStatus(StatusAcompanhamento.ATIVO);
        a.setDataInicio(LocalDate.now(RelogioConfig.FUSO).minusDays(10));
        return acompanhamentoRepository.save(a).getId();
    }

    Long novoMentor() {
        Mentor m = new Mentor();
        m.setNome("Mentora dos Anexos");
        m.setValorSessao(new BigDecimal("100.00"));
        m.setDuracaoSessaoMinutos(60);
        return mentorRepository.save(m).getId();
    }

    Long novaStartup(String nome) {
        Startup s = new Startup();
        s.setNome(nome);
        return startupRepository.save(s).getId();
    }
}
