package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.precificacao.ResultadoPrecificacao;
import br.com.srm.creditengine.taxas.TaxaBase;
import br.com.srm.creditengine.taxas.TaxaCambio;

import java.math.BigDecimal;

public record PrecificacaoCalculada(
        int prazoDias,
        TaxaBase taxaBase,
        BigDecimal spreadMensal,
        TaxaCambio cambio,
        boolean cambioDesatualizado,
        ResultadoPrecificacao resultado
) {
}
