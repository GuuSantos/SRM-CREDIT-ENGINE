package br.com.srm.creditengine.comum;

import org.springframework.http.HttpStatus;

public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected NegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    protected NegocioException(HttpStatus status, String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {return status;}
    public String getCodigo() {return codigo;}
}
