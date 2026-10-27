package br.com.srm.creditengine.taxas;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CadastroTaxaBaseRequest(
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("0.20")
        @Digits(integer = 3, fraction = 8)
        BigDecimal taxaMensal
) {
}
