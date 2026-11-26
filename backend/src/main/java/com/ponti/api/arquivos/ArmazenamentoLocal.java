package com.ponti.api.arquivos;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Armazenamento para o PC e para os testes: guarda os arquivos na memória da API.
// Imita o Supabase: o link de download é assinado e expira, então só quem recebeu o link
// (o mentor ou a startup do acompanhamento) consegue baixar.
@Component
@ConditionalOnProperty(name = "ponti.storage.tipo", havingValue = "local", matchIfMissing = true)
public class ArmazenamentoLocal implements ArmazenamentoArquivos {

    static final Duration VALIDADE_LINK = Duration.ofHours(1);

    record Arquivo(byte[] conteudo, String tipoConteudo) {
    }

    private final Map<String, Arquivo> arquivos = new ConcurrentHashMap<>();
    private final byte[] chave = new byte[32];
    private final Clock relogio;

    public ArmazenamentoLocal(Clock relogio) {
        this.relogio = relogio;
        new SecureRandom().nextBytes(chave);
    }

    @Override
    public void salvar(String caminho, byte[] conteudo, String tipoConteudo) {
        arquivos.put(caminho, new Arquivo(conteudo, tipoConteudo));
    }

    @Override
    public String linkTemporario(String caminho, String nomeParaBaixar) {
        long expira = relogio.instant().plus(VALIDADE_LINK).getEpochSecond();
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/arquivos-locais").build().toUriString()
                + "?caminho=" + codificar(caminho)
                + "&expira=" + expira
                + "&nome=" + codificar(nomeParaBaixar)
                + "&assinatura=" + assinar(caminho, expira);
    }

    @Override
    public void apagar(Collection<String> caminhos) {
        caminhos.forEach(arquivos::remove);
    }

    // Usado por ArquivoLocalController: devolve o arquivo só se a assinatura confere e o link não expirou
    Arquivo abrir(String caminho, long expira, String assinatura) {
        if (expira < relogio.instant().getEpochSecond()) return null;
        if (!java.security.MessageDigest.isEqual(assinar(caminho, expira).getBytes(), assinatura.getBytes())) return null;
        return arquivos.get(caminho);
    }

    boolean existe(String caminho) {
        return arquivos.containsKey(caminho);
    }

    private String assinar(String caminho, long expira) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            byte[] assinatura = mac.doFinal((caminho + "|" + expira).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
