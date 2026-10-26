package br.com.srm.creditengine.precificacao;

import java.math.BigDecimal;

public record EntradaPrecificacao(
        TipoRecebivel tipo,
        BigDecimal valorFace,
        BigDecimal prazoMeses,
        BigDecimal taxaBaseMensal,
        Moeda moedaPagamento,
        BigDecimal cambioBrlPorUsd
) {

}
