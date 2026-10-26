package br.com.srm.creditengine.precificacao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenCasesTest {

    private static final BigDecimal TAXA_BASE = new BigDecimal("0.01");

    private final MotorPrecificacao motor = new MotorPrecificacao(
            List.of(new DuplicataMercantil(), new ChequePreDatado()));

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "C1, DUPLICATA_MERCANTIL, 100000.00, 3, BRL,       , 92859.94, 7140.06",
            "C2, CHEQUE_PRE_DATADO,    25000.00, 2, BRL,       , 23337.77, 1662.23",
            "C3, DUPLICATA_MERCANTIL, 100000.00, 3, USD, 5.4321, 17094.67, 7140.06"
    })
    void reproduzGoldenCasesAoCentavo(String caso, TipoRecebivel tipo, BigDecimal valorFace,
                                      BigDecimal prazoMeses, Moeda moedaPagamento, BigDecimal cambio,
                                      BigDecimal valorPagamentoEsperado, BigDecimal desagioEsperado) {

        var entrada = new EntradaPrecificacao(tipo, valorFace, prazoMeses, TAXA_BASE, moedaPagamento, cambio);

        var resultado = motor.precificar(entrada);

        assertThat(resultado.valorPagamento()).isEqualTo(valorPagamentoEsperado);
        assertThat(resultado.desagio()).isEqualTo(desagioEsperado);
    }
}