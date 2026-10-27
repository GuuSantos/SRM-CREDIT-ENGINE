package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AtualizadorCambio {

    private final ClienteProvedorCambio clienteProvedor;
    private final TaxaService taxaService;

    public AtualizadorCambio(ClienteProvedorCambio clienteProvedor, TaxaService taxaService) {
        this.clienteProvedor = clienteProvedor;
        this.taxaService = taxaService;
    }

    public TaxaCambio atualizar(String usuario) {
        BigDecimal cotacao = clienteProvedor.buscarCotacao(Moeda.USD);
        return taxaService.cadastrarCambio(cotacao, FonteCambio.PROVEDOR, usuario);
    }
}