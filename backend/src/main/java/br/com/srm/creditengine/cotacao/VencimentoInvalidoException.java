package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.NegocioException;
import org.springframework.http.HttpStatus;

public class VencimentoInvalidoException extends NegocioException {

    protected VencimentoInvalidoException(String mensagem) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "COTACAO_EXPIRADA", mensagem);
    }
}
