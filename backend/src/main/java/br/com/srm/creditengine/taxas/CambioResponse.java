package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record CambioResponse(
        Long id,
        Moeda moeda,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal brlPorUnidade,
        Instant vigenteDesde,
        FonteCambio fonte,
        String criadoPor,
        boolean desatualizado
) {
    static CambioResponse de(TaxaCambio cambio, boolean desatualizado){
        return new CambioResponse(cambio.getId(), cambio.getMoeda(), cambio.getBrlPorUnidade(), cambio.getVigenteDesde(),
                            cambio.getFonte(), cambio.getCriadoPor(), desatualizado);
    }
}
