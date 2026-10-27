package br.com.srm.creditengine.extrato;

import br.com.srm.creditengine.precificacao.Moeda;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record FiltroExtrato(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        @Pattern(regexp = "\\d{14}", message = "deve conter exatamente 14 dígitos") String cedenteCnpj,
        Moeda moeda,
        @Min(0) Integer pagina,
        @Min(1) @Max(100) Integer tamanho,
        OrdenacaoExtrato ordenarPor,
        DirecaoOrdenacao direcao
) {
    public FiltroExtrato {
        if (pagina == null) pagina = 0;
        if (tamanho == null) tamanho = 50;
        if (ordenarPor == null) ordenarPor = OrdenacaoExtrato.LIQUIDADO_EM;
        if (direcao == null) direcao = DirecaoOrdenacao.DESC;
    }

    @AssertTrue(message = "dataFim deve ser igual ou posterior a dataInicio")
    public boolean isPeriodoValido() {
        return dataInicio == null || dataFim == null || !dataFim.isBefore(dataInicio);
    }
}