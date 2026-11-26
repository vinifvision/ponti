package com.ponti.api.arquivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Confere se o backend conversa com a API do Supabase Storage do jeito certo
// (endereços, chave nos cabeçalhos e formato do link assinado), sem acessar a internet:
// o MockRestServiceServer faz o papel do Supabase.
class ArmazenamentoSupabaseTest {

    static final String BASE = "https://exemplo.supabase.co/storage/v1";

    MockRestServiceServer supabase;
    ArmazenamentoSupabase armazenamento;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        supabase = MockRestServiceServer.bindTo(builder).build();
        armazenamento = new ArmazenamentoSupabase(builder, "https://exemplo.supabase.co/", "chave-de-teste", "anexos");
    }

    @Test
    void enviaOArquivoParaOBucketComAChave() {
        supabase.expect(requestTo(BASE + "/object/anexos/acompanhamentos/7/abc.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-de-teste"))
                .andExpect(header("apikey", "chave-de-teste"))
                .andExpect(header("Content-Type", "application/pdf"))
                .andRespond(withSuccess("{\"Key\":\"anexos/acompanhamentos/7/abc.pdf\"}", MediaType.APPLICATION_JSON));

        armazenamento.salvar("acompanhamentos/7/abc.pdf", "%PDF-1.4".getBytes(), "application/pdf");
        supabase.verify();
    }

    @Test
    void geraLinkAssinadoQueExpiraEmUmaHora() {
        supabase.expect(requestTo(BASE + "/object/sign/anexos/acompanhamentos/7/abc.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"expiresIn\":3600}"))
                .andRespond(withSuccess("{\"signedURL\":\"/object/sign/anexos/acompanhamentos/7/abc.pdf?token=xyz\"}",
                        MediaType.APPLICATION_JSON));

        String link = armazenamento.linkTemporario("acompanhamentos/7/abc.pdf", "Plano final.pdf");

        assertThat(link).isEqualTo(BASE + "/object/sign/anexos/acompanhamentos/7/abc.pdf?token=xyz&download=Plano%20final.pdf");
        supabase.verify();
    }

    @Test
    void apagaVariosArquivosDeUmaVez() {
        supabase.expect(requestTo(BASE + "/object/anexos"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"prefixes\":[\"acompanhamentos/7/a.pdf\",\"acompanhamentos/7/b.png\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        armazenamento.apagar(List.of("acompanhamentos/7/a.pdf", "acompanhamentos/7/b.png"));
        supabase.verify();
    }

    @Test
    void naoSobeSemAsVariaveisDeAmbiente() {
        assertThatThrownBy(() -> new ArmazenamentoSupabase(RestClient.builder(), "", "", "anexos"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PONTI_SUPABASE_URL");
    }
}
