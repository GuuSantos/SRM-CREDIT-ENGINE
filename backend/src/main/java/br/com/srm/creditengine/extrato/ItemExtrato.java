package br.com.srm.creditengine.extrato;

import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemExtrato(
        Long id,
        String cotacaoId,
        String cedenteCnpj,
        String cedenteRazaoSocial,
        String numeroTitulo,
        TipoRecebivel tipo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorFace,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPresenteBrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal desagio,
        Moeda moedaPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorPagamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cambioBrlPorUsd,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal taxaBaseMensal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spreadMensal,
        Integer prazoDias,
        String liquidadoPor,
        Instant liquidadoEm
) {
}