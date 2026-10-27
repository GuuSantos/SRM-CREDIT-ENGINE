package br.com.srm.creditengine.liquidacao;

import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LiquidacaoResponse(
        Long id,
        UUID cotacaoId,
        Long recebivelId,
        Long cedenteId,
        TipoRecebivel tipo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorFace,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPresenteBrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal desagio,
        Moeda moedaPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cambioBrlPorUsd,
        String liquidadoPor,
        Instant liquidadoEm
) {
    static LiquidacaoResponse de(Liquidacao liquidacao) {
        return new LiquidacaoResponse(
                liquidacao.getId(),
                liquidacao.getCotacaoId(),
                liquidacao.getRecebivelId(),
                liquidacao.getCedenteId(),
                liquidacao.getTipo(),
                liquidacao.getValorFace(),
                liquidacao.getValorPresenteBrl(),
                liquidacao.getDesagio(),
                liquidacao.getMoedaPagamento(),
                liquidacao.getValorPagamento(),
                liquidacao.getCambioBrlPorUsd(),
                liquidacao.getLiquidadoPor(),
                liquidacao.getLiquidadoEm());
    }
}