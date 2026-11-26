package com.ponti.api.arquivos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Download dos arquivos guardados no ArmazenamentoLocal (só existe no PC e nos testes).
// Não pede login (o link vai num <a href>, que não leva o token): quem protege é a assinatura do link.
@RestController
@ConditionalOnProperty(name = "ponti.storage.tipo", havingValue = "local", matchIfMissing = true)
public class ArquivoLocalController {

    private final ArmazenamentoLocal armazenamento;

    public ArquivoLocalController(ArmazenamentoLocal armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/arquivos-locais")
    public ResponseEntity<byte[]> baixar(@RequestParam String caminho,
                                         @RequestParam long expira,
                                         @RequestParam String nome,
                                         @RequestParam String assinatura) {
        ArmazenamentoLocal.Arquivo arquivo = armazenamento.abrir(caminho, expira, assinatura);
        if (arquivo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado ou link expirado.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nome, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo.conteudo());
    }
}
