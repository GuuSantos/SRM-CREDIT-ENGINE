package br.com.srm.creditengine.recebivel;

import br.com.srm.creditengine.NegocioException;
import org.springframework.http.HttpStatus;

public class RecebivelIndisponivelException extends NegocioException {
    public RecebivelIndisponivelException(String message) {
        super(HttpStatus.CONFLICT,"RECEBIVEL_INDISPONIVEL",message);
    }
}
