package br.com.srm.creditengine.recebivel;

import br.com.srm.creditengine.comum.NegocioException;
import org.springframework.http.HttpStatus;

public class CedenteNaoEncontradoException extends NegocioException {
    public CedenteNaoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND,"CEDENTE_NAO_ENCONTRADO",message);
    }
}
