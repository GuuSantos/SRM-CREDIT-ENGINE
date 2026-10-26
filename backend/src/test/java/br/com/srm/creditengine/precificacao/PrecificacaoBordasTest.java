package br.com.srm.creditengine.precificacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecificacaoBordasTest {

    private static final BigDecimal TAXA_BASE = new BigDecimal("0.01");
    private static final BigDecimal FACE = new BigDecimal("100000.00");

    private final MotorPrecificacao motor = new MotorPrecificacao(
            List.of(new DuplicataMercantil(), new ChequePreDatado()));

    @Test
    void prazoFracionarioUsaExpoenteDecimal() {
        // 45 dias = 1,5 mês. Referência calculada à parte: 100000 / (1,025 × √1,025)
        var entrada = new EntradaPrecificacao(TipoRecebivel.DUPLICATA_MERCANTIL, FACE,
                new BigDecimal("1.5"), TAXA_BASE, Moeda.BRL, null);

        assertThat(motor.precificar(entrada).valorPresenteBrl()).isEqualTo(new BigDecimal("96363.86"));
    }

    @Test
    void conversaoNoEmpateArredondaParaOParMaisProximo() {
        // 92859.94 / 4 = 23214.985 → HALF_EVEN dá 23214.98 (HALF_UP daria 23214.99)
        var entrada = new EntradaPrecificacao(TipoRecebivel.DUPLICATA_MERCANTIL, FACE,
                new BigDecimal("3"), TAXA_BASE, Moeda.USD, new BigDecimal("4"));

        assertThat(motor.precificar(entrada).valorPagamento()).isEqualTo(new BigDecimal("23214.98"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0333", "1", "1.5", "12", "60"})
    void valorPresenteNuncaSuperaOValorDeFace(String prazoMeses) {
        var entrada = new EntradaPrecificacao(TipoRecebivel.CHEQUE_PRE_DATADO, FACE,
                new BigDecimal(prazoMeses), BigDecimal.ZERO, Moeda.BRL, null);

        var valorPresente = motor.precificar(entrada).valorPresenteBrl();

        assertThat(valorPresente).isPositive().isLessThan(FACE);
    }

    @Test
    void pagamentoEmUsdSemCambioEhRecusado() {
        var entrada = new EntradaPrecificacao(TipoRecebivel.DUPLICATA_MERCANTIL, FACE,
                new BigDecimal("3"), TAXA_BASE, Moeda.USD, null);

        assertThatThrownBy(() -> motor.precificar(entrada))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tipoSemEstrategiaEhRecusado() {
        var motorSoComDuplicata = new MotorPrecificacao(List.of(new DuplicataMercantil()));
        var entrada = new EntradaPrecificacao(TipoRecebivel.CHEQUE_PRE_DATADO, FACE,
                new BigDecimal("2"), TAXA_BASE, Moeda.BRL, null);

        assertThatThrownBy(() -> motorSoComDuplicata.precificar(entrada))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estrategiaDuplicadaParaOMesmoTipoFalhaNaCriacao() {
        assertThatThrownBy(() -> new MotorPrecificacao(
                List.of(new DuplicataMercantil(), new DuplicataMercantil())))
                .isInstanceOf(IllegalStateException.class);
    }
}