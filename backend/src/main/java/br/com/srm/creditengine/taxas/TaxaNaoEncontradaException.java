package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.NegocioException;
import org.springframework.http.HttpStatus;

public class TaxaNaoEncontradaException extends NegocioException {
    public TaxaNaoEncontradaException(String mensagem) {
        super(HttpStatus.NOT_FOUND,"TAXA_NAO_ENCONTRADA",mensagem);
    }
}
