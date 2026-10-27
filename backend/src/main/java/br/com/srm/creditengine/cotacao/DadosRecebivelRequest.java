package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosRecebivelRequest(

        @NotNull
        Long cedenteId,

        @NotNull
        TipoRecebivel tipo,

        @NotBlank
        @Size(max = 30)
        String numero,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax("1000000000.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorFace,

        @NotNull
        LocalDate vencimento,

        @NotNull
        Moeda moedaPagamento
) {
}
