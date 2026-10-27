package br.com.srm.creditengine.taxas;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CadastroCambioRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 6, fraction = 8)
        BigDecimal brlPorUnidade
) {
}
