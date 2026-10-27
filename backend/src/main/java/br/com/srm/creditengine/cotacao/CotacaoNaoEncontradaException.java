package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.comum.NegocioException;
import org.springframework.http.HttpStatus;

public class CotacaoNaoEncontradaException extends NegocioException {
    public CotacaoNaoEncontradaException(String message) {
        super(HttpStatus.NOT_FOUND,"COTACAO_NAO_ENCONTRADA",message);
    }
}
