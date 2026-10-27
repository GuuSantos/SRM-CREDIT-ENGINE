package br.com.srm.creditengine.cotacao;

public class CotacaoExpiradaException extends RuntimeException {
    public CotacaoExpiradaException(String message) {
        super(message);
    }
}
