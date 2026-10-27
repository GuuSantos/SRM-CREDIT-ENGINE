package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteProvedorCambioTest {

    @Test
    void recuperaDepoisDeFalhasTemporarias() {
        AtomicInteger chamadas = new AtomicInteger();
        ProvedorCambio instavel = moeda -> {
            if (chamadas.incrementAndGet() < 3) {
                throw new IllegalStateException("fora do ar");
            }
            return new BigDecimal("5.4321");
        };
        var cliente = new ClienteProvedorCambio(instavel, Duration.ofSeconds(1), Duration.ZERO);

        BigDecimal cotacao = cliente.buscarCotacao(Moeda.USD);

        assertThat(cotacao).isEqualByComparingTo(new BigDecimal("5.4321"));
        assertThat(chamadas.get()).isEqualTo(3);
    }

    @Test
    void desisteQuandoTodasAsTentativasEstouramOTimeout() {
        AtomicInteger chamadas = new AtomicInteger();
        ProvedorCambio lento = moeda -> {
            chamadas.incrementAndGet();
            dormir(500);
            return new BigDecimal("5.4321");
        };
        var cliente = new ClienteProvedorCambio(lento, Duration.ofMillis(100), Duration.ZERO);

        assertThatThrownBy(() -> cliente.buscarCotacao(Moeda.USD))
                .isInstanceOf(ProvedorCambioIndisponivelException.class);
        assertThat(chamadas.get()).isEqualTo(3);
    }

    @Test
    void trataCotacaoInvalidaComoFalha() {
        var cliente = new ClienteProvedorCambio(moeda -> BigDecimal.ZERO, Duration.ofSeconds(1), Duration.ZERO);

        assertThatThrownBy(() -> cliente.buscarCotacao(Moeda.USD))
                .isInstanceOf(ProvedorCambioIndisponivelException.class);
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}