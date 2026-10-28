package com.ponti.api.erro;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Transforma todo erro da API no formato único de Problems (Problem Details), em português.
// Erros de validação trazem também a lista "campos": [{ "campo": "...", "mensagem": "..." }].
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    // @Valid no corpo da requisição
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        List<Map<String, String>> campos = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(TratadorDeErros::campo)
                .toList();
        List<String> gerais = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        return validacao(campos, gerais, request);
    }

    // Validação de parâmetros e listas (ex.: lista de faixas da disponibilidade)
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException ex,
                                                                            @NonNull HttpHeaders headers,
                                                                            @NonNull HttpStatusCode status,
                                                                            @NonNull WebRequest request) {
        List<String> mensagens = ex.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList();
        return validacao(List.of(), mensagens, request);
    }

    // JSON mal formado ou valor de tipo errado (ex.: texto onde se espera número)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        ProblemDetail problema = Problemas.criar(HttpStatus.BAD_REQUEST,
                "O corpo da requisição tem um formato inválido. Confira os tipos dos campos.", rota(request));
        return ResponseEntity.badRequest().body(problema);
    }

    // Qualquer erro não previsto: registra no log e não expõe detalhes internos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex, WebRequest request) {
        log.error("Erro inesperado em {}", rota(request), ex);
        ProblemDetail problema = Problemas.criar(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente.", rota(request));
        return ResponseEntity.internalServerError().body(problema);
    }

    // Todo erro tratado pelo Spring (404, 405, ResponseStatusException...) passa aqui antes de virar resposta:
    // garante título em português e o campo "message".
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, @NonNull HttpHeaders headers,
                                                          @NonNull HttpStatusCode statusCode,
                                                          @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problema) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            if (status != null) {
                problema.setTitle(Problemas.titulo(status));
            }
            String detalhe = problema.getDetail() != null ? problema.getDetail() : problema.getTitle();
            problema.setDetail(detalhe);
            problema.setProperty("message", detalhe);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> validacao(List<Map<String, String>> campos, List<String> gerais, WebRequest request) {
        String primeira = !campos.isEmpty()
                ? campos.get(0).get("campo") + ": " + campos.get(0).get("mensagem")
                : gerais.stream().findFirst().orElse("Dados inválidos");
        ProblemDetail problema = Problemas.criar(HttpStatus.BAD_REQUEST, primeira, rota(request));
        problema.setProperty("campos", campos);
        if (!gerais.isEmpty()) {
            problema.setProperty("erros", gerais);
        }
        return ResponseEntity.badRequest().body(problema);
    }

    private static Map<String, String> campo(FieldError erro) {
        return Map.of("campo", erro.getField(), "mensagem", String.valueOf(erro.getDefaultMessage()));
    }

    private static String rota(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
