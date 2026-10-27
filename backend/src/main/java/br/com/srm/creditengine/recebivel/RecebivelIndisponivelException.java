package br.com.srm.creditengine.recebivel;

public class RecebivelIndisponivelException extends RuntimeException {
    public RecebivelIndisponivelException(String message) {
        super(message);
    }
}
