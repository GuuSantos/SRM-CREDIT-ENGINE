package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.comum.NegocioException;
import org.springframework.http.HttpStatus;

public class CotacaoDeOutroUsuarioException extends NegocioException {
    public CotacaoDeOutroUsuarioException(String message) {
        super(HttpStatus.FORBIDDEN,"COTACAO_DE_OUTRO_OPERADOR",message);
    }
}
