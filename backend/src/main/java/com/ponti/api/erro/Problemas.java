package com.ponti.api.erro;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

// Formato único de erro da API (RFC 9457, "Problem Details"):
// { "type", "title", "status", "detail", "instance", "message" }
// "message" repete o "detail" para o front ler sempre o mesmo campo.
public final class Problemas {

    private Problemas() {
    }

    public static ProblemDetail criar(HttpStatus status, String mensagem, String rota) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setTitle(titulo(status));
        if (rota != null) {
            problema.setInstance(URI.create(rota));
        }
        problema.setProperty("message", mensagem);
        return problema;
    }

    public static String titulo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Dados inválidos";
            case UNAUTHORIZED -> "Login necessário";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Não encontrado";
            case CONFLICT -> "Conflito";
            case METHOD_NOT_ALLOWED -> "Operação não permitida";
            case PAYMENT_REQUIRED -> "Pagamento recusado";
            case UNPROCESSABLE_ENTITY -> "Não foi possível concluir";
            default -> status.is5xxServerError() ? "Erro interno" : status.getReasonPhrase();
        };
    }
}
