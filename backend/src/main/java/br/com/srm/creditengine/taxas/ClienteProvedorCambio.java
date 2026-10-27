package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ClienteProvedorCambio {

    private static final Logger log = LoggerFactory.getLogger(ClienteProvedorCambio.class);
    static final int MAX_TENTATIVAS = 3;

    private final ProvedorCambio provedor;
    private final Duration timeout;
    private final Duration esperaEntreTentativas;

    public ClienteProvedorCambio(ProvedorCambio provedor,
                                 @Value("${cambio.provedor.timeout:PT2S}") Duration timeout,
                                 @Value("${cambio.provedor.espera-entre-tentativas:PT0.5S}") Duration esperaEntreTentativas) {
        this.provedor = provedor;
        this.timeout = timeout;
        this.esperaEntreTentativas = esperaEntreTentativas;
    }

    public BigDecimal buscarCotacao(Moeda moeda) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return CompletableFuture
                        .supplyAsync(() -> validar(provedor.cotacaoAtual(moeda)))
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProvedorCambioIndisponivelException("Consulta ao provedor interrompida", e);
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Falha ao consultar o provedor de câmbio (tentativa {}/{}): {}",
                        tentativa, MAX_TENTATIVAS, e.toString());
                if (tentativa < MAX_TENTATIVAS) {
                    aguardar(esperaEntreTentativas.multipliedBy(tentativa));
                }
            }
        }
        throw new ProvedorCambioIndisponivelException(
                "Provedor de câmbio indisponível após " + MAX_TENTATIVAS + " tentativas");
    }

    private static BigDecimal validar(BigDecimal cotacao) {
        if (cotacao == null || cotacao.signum() <= 0) {
            throw new IllegalStateException("Cotação inválida recebida do provedor: " + cotacao);
        }
        return cotacao;
    }

    private static void aguardar(Duration duracao) {
        try {
            Thread.sleep(duracao.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProvedorCambioIndisponivelException("Espera entre tentativas interrompida", e);
        }
    }
}