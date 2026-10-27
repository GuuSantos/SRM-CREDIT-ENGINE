package br.com.srm.creditengine.taxas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AtualizacaoCambioJob {

    private static final Logger log = LoggerFactory.getLogger(AtualizacaoCambioJob.class);

    private final AtualizadorCambio atualizador;

    public AtualizacaoCambioJob(AtualizadorCambio atualizador) {
        this.atualizador = atualizador;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${cambio.job.intervalo:PT1H}")
    public void executar() {
        try {
            TaxaCambio cambio = atualizador.atualizar("sistema");
            log.info("Câmbio atualizado pelo provedor: {}", cambio.getBrlPorUnidade());
        } catch (ProvedorCambioIndisponivelException e) {
            log.error("Job de câmbio falhou; a taxa vigente foi mantida: {}", e.getMessage());
        }
    }
}