package br.com.srm.creditengine.precificacao;

import java.math.BigDecimal;

public record ResultadoPrecificacao(
        BigDecimal valorPresenteBrl,
        BigDecimal desagio,
        Moeda moedaPagamento,
        BigDecimal valorPagamento
) {
}
