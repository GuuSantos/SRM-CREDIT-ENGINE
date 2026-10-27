package br.com.srm.creditengine.recebivel;

public class CedenteNaoEncontradoException extends RuntimeException {
    public CedenteNaoEncontradoException(String message) {
        super(message);
    }
}
