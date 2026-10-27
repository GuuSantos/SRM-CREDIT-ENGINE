package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.NegocioException;
import org.springframework.http.HttpStatus;

public class CotacaoExpiradaException extends NegocioException {
    public CotacaoExpiradaException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,"COTACAO_EXPIRADA",message);
    }
}
