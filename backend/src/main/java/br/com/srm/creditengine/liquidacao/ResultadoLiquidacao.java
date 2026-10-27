package br.com.srm.creditengine.liquidacao;

public record ResultadoLiquidacao(Liquidacao liquidacao, boolean criada) {
    static ResultadoLiquidacao nova(Liquidacao liquidacao){
        return new ResultadoLiquidacao(liquidacao, true);
    }

    static ResultadoLiquidacao existente(Liquidacao liquidacao){
        return new ResultadoLiquidacao(liquidacao, false);
    }
}
