package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class ProvedorCambioMock implements ProvedorCambio {

    private final BigDecimal valor;
    private final Duration atraso;
    private final boolean falhar;

    public ProvedorCambioMock(@Value("${cambio.mock.valor:5.4321}") BigDecimal valor,
                              @Value("${cambio.mock.atraso:PT0S}") Duration atraso,
                              @Value("${cambio.mock.falhar:false}") boolean falhar) {
        this.valor = valor;
        this.atraso = atraso;
        this.falhar = falhar;
    }

    @Override
    public BigDecimal cotacaoAtual(Moeda moeda) {
        if (falhar) {
            throw new IllegalStateException("Provedor de câmbio indisponível (simulado)");
        }
        dormir(atraso);
        return valor;
    }

    private static void dormir(Duration duracao) {
        if (duracao.isZero()) {
            return;
        }
        try {
            Thread.sleep(duracao.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta interrompida", e);
        }
    }
}