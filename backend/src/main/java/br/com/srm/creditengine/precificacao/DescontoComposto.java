package br.com.srm.creditengine.precificacao;

import ch.obermuhlner.math.big.BigDecimalMath;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

final class DescontoComposto {
    static final MathContext PRECISAO = MathContext.DECIMAL128;

    private DescontoComposto() {}

    static BigDecimal valorPresente(BigDecimal valorFace, BigDecimal taxaMensal, BigDecimal prazoMeses){
        BigDecimal fator = BigDecimal.ONE.add(taxaMensal);
        BigDecimal fatorNoPrazo = elevar(fator, prazoMeses);
        return valorFace.divide(fatorNoPrazo, PRECISAO);
    }

    static BigDecimal elevar(BigDecimal base, BigDecimal expoente) {
        BigDecimal parteInteira = expoente.setScale(0, RoundingMode.DOWN);
        BigDecimal parteFracionaria = expoente.subtract(parteInteira);

        BigDecimal resultado = base.pow(parteInteira.intValueExact(), PRECISAO);

        if (parteFracionaria.signum() != 0) {
            BigDecimal fracao = BigDecimalMath.pow(base, parteFracionaria, PRECISAO);
            resultado = resultado.multiply(fracao, PRECISAO);
        }
        return resultado;
    }
}
