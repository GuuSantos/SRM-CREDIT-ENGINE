package br.com.srm.creditengine.taxas;

public record TaxasVigentesResponse(
        TaxaBaseResponse taxaBase, CambioResponse cambioUsd
) {
}
