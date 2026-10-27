package br.com.srm.creditengine.cotacao;

public class CotacaoNaoEncontradaException extends RuntimeException {
    public CotacaoNaoEncontradaException(String message) {
        super(message);
    }
}
