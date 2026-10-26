package br.com.srm.creditengine.precificacao;

import java.math.BigDecimal;

public class DuplicataMercantil implements EstrategiaPrecificacao {

    private static final BigDecimal SPREAD_MENSAL = new BigDecimal("0.015");

    @Override
    public TipoRecebivel tipo() {
        return TipoRecebivel.DUPLICATA_MERCANTIL;
    }

    @Override
    public BigDecimal spreadMensal() {
        return SPREAD_MENSAL;
    }

    @Override
    public BigDecimal calcularValorPresente(BigDecimal valorFace, BigDecimal taxaBaseMensal, BigDecimal prazoMeses) {
        BigDecimal taxaTotal = taxaBaseMensal.add(SPREAD_MENSAL);
        return DescontoComposto.valorPresente(valorFace, taxaTotal, prazoMeses);
    }
}
