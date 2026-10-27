package br.com.srm.creditengine.cotacao;

public class VencimentoInvalidoException extends RuntimeException{
    public VencimentoInvalidoException(String mensagem){
        super(mensagem);
    }
}
