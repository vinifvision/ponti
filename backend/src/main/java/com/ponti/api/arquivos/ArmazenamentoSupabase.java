package com.ponti.api.arquivos;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Supabase Storage: guarda os anexos num bucket PRIVADO (ninguém baixa sem um link assinado).
// Fala com a API REST do Storage usando a chave secreta do projeto (service_role), que fica só
// na variável de ambiente do Render. Não precisa de biblioteca nova: o RestClient já vem no Spring.
//
// Ligado quando ponti.storage.tipo=supabase (o perfil prod já faz isso).
@Component
@ConditionalOnProperty(name = "ponti.storage.tipo", havingValue = "supabase")
public class ArmazenamentoSupabase implements ArmazenamentoArquivos {

    // Quanto tempo o link de download vale. Cada vez que a tela carrega, um link novo é gerado.
    static final Duration VALIDADE_LINK = Duration.ofHours(1);

    private final RestClient http;
    private final String urlBase;
    private final String bucket;

    public ArmazenamentoSupabase(RestClient.Builder builder,
                                 @Value("${ponti.storage.supabase.url}") String url,
                                 @Value("${ponti.storage.supabase.chave}") String chave,
                                 @Value("${ponti.storage.bucket:anexos}") String bucket) {
        if (url == null || url.isBlank() || chave == null || chave.isBlank()) {
            // Melhor a API não subir do que subir guardando arquivos no lugar errado
            throw new IllegalStateException("Configure PONTI_SUPABASE_URL e PONTI_SUPABASE_CHAVE para usar o Supabase Storage.");
        }
        this.urlBase = url.replaceAll("/+$", "") + "/storage/v1";
        this.bucket = bucket;
        this.http = builder
                .baseUrl(urlBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + chave)
                .defaultHeader("apikey", chave)
                .build();
    }

    // Os endereços são montados como texto (e não como modelo "{caminho}") para as barras do caminho
    // não virarem %2F. Não há risco: o caminho é gerado pelo servidor (pasta + UUID + extensão).
    @Override
    public void salvar(String caminho, byte[] conteudo, String tipoConteudo) {
        http.post()
                .uri(urlBase + "/object/" + bucket + "/" + caminho)
                .contentType(MediaType.parseMediaType(tipoConteudo))
                .body(conteudo)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String linkTemporario(String caminho, String nomeParaBaixar) {
        RespostaAssinatura resposta = http.post()
                .uri(urlBase + "/object/sign/" + bucket + "/" + caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", VALIDADE_LINK.toSeconds()))
                .retrieve()
                .body(RespostaAssinatura.class);
        if (resposta == null || resposta.signedURL() == null) {
            throw new IllegalStateException("O Supabase não devolveu o link do arquivo.");
        }
        // "download" faz o navegador baixar o arquivo com o nome original, em vez de abrir na aba
        return urlBase + resposta.signedURL() + "&download="
                + URLEncoder.encode(nomeParaBaixar, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public void apagar(Collection<String> caminhos) {
        if (caminhos.isEmpty()) return;
        http.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/object/{bucket}", bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("prefixes", List.copyOf(caminhos)))
                .retrieve()
                .toBodilessEntity();
    }

    record RespostaAssinatura(String signedURL) {
    }
}
