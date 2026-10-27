package br.com.srm.creditengine.taxas;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record TaxaBaseResponse(
        Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal taxaMensal,
        Instant vigenteDesde,
        String criadoPor
) {
    static TaxaBaseResponse de(TaxaBase taxa) {
        return new TaxaBaseResponse(taxa.getId(), taxa.getTaxaMensal(), taxa.getVigenteDesde(), taxa.getCriadoPor());
    }
}
