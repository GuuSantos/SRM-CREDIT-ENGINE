package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.precificacao.Moeda;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CotacaoResponse(
        UUID id,
        Long recebivelId,
        int prazoDias,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal taxaBaseMensal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spreadMensal,
        Moeda moedaPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cambioBrlPorUsd,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPresenteBrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal desagio,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPagamento,
        Instant expiraEm
) {
    static CotacaoResponse de(Cotacao cotacao) {
        return new CotacaoResponse(
                cotacao.getId(),
                cotacao.getRecebivel().getId(),
                cotacao.getPrazoDias(),
                cotacao.getTaxaBaseMensal(),
                cotacao.getSpreadMensal(),
                cotacao.getMoedaPagamento(),
                cotacao.getCambioBrlPorUsd(),
                cotacao.getValorPresenteBrl(),
                cotacao.getDesagio(),
                cotacao.getValorPagamento(),
                cotacao.getExpiraEm());
    }
}