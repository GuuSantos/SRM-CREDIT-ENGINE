package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;

import java.math.BigDecimal;


@FunctionalInterface
public interface ProvedorCambio {
    BigDecimal cotacaoAtual(Moeda moeda);
}
