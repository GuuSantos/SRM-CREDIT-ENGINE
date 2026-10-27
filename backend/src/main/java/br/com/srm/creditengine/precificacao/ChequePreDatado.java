package br.com.srm.creditengine.precificacao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class ChequePreDatado implements EstrategiaPrecificacao {

    private static final BigDecimal SPREAD_MENSAL = new BigDecimal("0.025");

    @Override
    public TipoRecebivel tipo() {
        return TipoRecebivel.CHEQUE_PRE_DATADO;
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
