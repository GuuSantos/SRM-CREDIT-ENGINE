package br.com.srm.creditengine.taxas;

public class TaxaNaoEncontradaException extends RuntimeException {
    public TaxaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
