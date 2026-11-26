package com.ponti.api.erro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Arquivo acima de 10 MB: o servidor corta o envio antes das regras do AnexoService.
// O erro precisa chegar ao front em português, no formato padrão.
class LimiteDeArquivoTest {

    @Test
    void arquivoGrandeDemaisVoltaEmPortugues() throws Exception {
        ResponseEntity<Object> resposta = new TratadorDeErros().handleException(
                new MaxUploadSizeExceededException(10L * 1024 * 1024),
                new ServletWebRequest(new MockHttpServletRequest("POST", "/atividades/1/entrega")));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat(problema.getTitle()).isEqualTo("Arquivo grande demais");
        assertThat(problema.getProperties()).containsEntry("message", "Cada arquivo pode ter até 10 MB.");
    }
}
