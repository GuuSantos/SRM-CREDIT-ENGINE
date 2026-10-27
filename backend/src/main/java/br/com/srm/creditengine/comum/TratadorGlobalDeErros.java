package br.com.srm.creditengine.comum;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail tratarNegocio(NegocioException e){
        log.warn("Regra de Negócio violada [{}]: {}", e.getCodigo(), e.getMessage());
        return problema(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail tratarConflito(RuntimeException e){
        log.warn("Conflito de dados: {}", e.getMessage());
        return problema(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS", "A operação conflitou com outra alteração simultânea; tente novamente");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarInesperado(Exception e) {
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElseGet(() -> UUID.randomUUID().toString());
        log.error("Erro inesperado [traceId={}]", traceId, e);
        ProblemDetail problema = problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro inesperado; informe o traceId ao suporte");
        problema.setProperty("traceId", traceId);
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", Objects.requireNonNullElse(erro.getDefaultMessage(), "valor inválido")))
                .toList();

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Dados de entrada inválidos");
        problema.setProperty("campos", campos);
        return handleExceptionInternal(e, problema, headers, status, request);
    }

    private static ProblemDetail problema(HttpStatusCode status, String codigo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
