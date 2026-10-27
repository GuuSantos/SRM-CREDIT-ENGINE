package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.NegocioException;
import org.springframework.http.HttpStatus;

public class CambioDesatualizadoException extends NegocioException {
    public CambioDesatualizadoException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,"CAMBIO_DESATUALIZADO",message);
    }
}
