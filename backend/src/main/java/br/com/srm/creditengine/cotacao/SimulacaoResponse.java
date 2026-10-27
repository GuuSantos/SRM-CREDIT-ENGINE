package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.precificacao.Moeda;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record SimulacaoResponse(
        int prazoDias,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal taxaBaseMensal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spreadMensal,
        Moeda moedaPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cambioBrlPorUsd,
        Instant cambioVigenteDesde,
        boolean cambioDesatualizado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPresenteBrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal desagio,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPagamento
) {
    static SimulacaoResponse de(PrecificacaoCalculada calculo) {
        var resultado = calculo.resultado();
        var cambio = calculo.cambio();
        return new SimulacaoResponse(
                calculo.prazoDias(),
                calculo.taxaBase().getTaxaMensal(),
                calculo.spreadMensal(),
                resultado.moedaPagamento(),
                cambio == null ? null : cambio.getBrlPorUnidade(),
                cambio == null ? null : cambio.getVigenteDesde(),
                calculo.cambioDesatualizado(),
                resultado.valorPresenteBrl(),
                resultado.desagio(),
                resultado.valorPagamento());
    }
}