package br.com.srm.creditengine.taxas;

public class ProvedorCambioIndisponivelException extends RuntimeException {

    public ProvedorCambioIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ProvedorCambioIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}