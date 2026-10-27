package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.comum.NegocioException;
import org.springframework.http.HttpStatus;

public class ProvedorCambioIndisponivelException extends NegocioException {

    public ProvedorCambioIndisponivelException(String mensagem) {
        super(HttpStatus.SERVICE_UNAVAILABLE,"PROVEDOR_CAMBIO_INDISPONIVEL",mensagem);
    }

    public ProvedorCambioIndisponivelException(String mensagem, Throwable causa) {
        super(HttpStatus.SERVICE_UNAVAILABLE,"PROVEDOR_CAMBIO_INDISPONIVEL",mensagem, causa);
    }
}