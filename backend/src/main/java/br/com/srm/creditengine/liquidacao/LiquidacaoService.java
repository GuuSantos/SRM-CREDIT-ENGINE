package br.com.srm.creditengine.liquidacao;


import br.com.srm.creditengine.cotacao.*;
import br.com.srm.creditengine.recebivel.Recebivel;
import br.com.srm.creditengine.recebivel.RecebivelIndisponivelException;
import br.com.srm.creditengine.recebivel.RecebivelRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class LiquidacaoService {

    private static final Logger log = LoggerFactory.getLogger(LiquidacaoService.class);
    private static final String METRICA_LIQUIDACOES = "liquidacoes";
    private final MeterRegistry metricas;


    private final CotacaoRepository cotacaoRepository;
    private final RecebivelRepository recebivelRepository;
    private final LiquidacaoRepository liquidacaoRepository;
    private final TransactionTemplate transacao;
    private final Clock clock;

    public LiquidacaoService(CotacaoRepository cotacaoRepository, RecebivelRepository recebivelRepository,
                             LiquidacaoRepository liquidacaoRepository, TransactionTemplate transacao, Clock clock,
                                MeterRegistry metricas) {
        this.cotacaoRepository = cotacaoRepository;
        this.recebivelRepository = recebivelRepository;
        this.liquidacaoRepository = liquidacaoRepository;
        this.transacao = transacao;
        this.clock = clock;
        this.metricas = metricas;
    }

    public ResultadoLiquidacao liquidar(UUID cotacaoId, String usuario){
        Cotacao cotacao = cotacaoRepository.findById(cotacaoId)
                .orElseThrow(() -> new CotacaoNaoEncontradaException("Cotação " + cotacaoId + " não encontrada"));

        if(!cotacao.pertenceA(usuario)){
            throw new CotacaoDeOutroUsuarioException("A cotação " + cotacaoId + " pertence a outro operador");
        }

        Optional<Liquidacao> existente = liquidacaoRepository.findByCotacaoId(cotacaoId);
        if(existente.isPresent()){
            contar(cotacao, "existente");
            return ResultadoLiquidacao.existente(existente.get());
        }
        if(cotacao.expirada(clock.instant())){
            contar(cotacao, "expirada");
            throw new CotacaoExpiradaException("A Cotação: "+ cotacaoId +"expirou; cote novamente!");
        }
        try {
            Liquidacao nova = transacao.execute(status -> efetivar(cotacaoId, usuario));
            contar(cotacao, "nova");
            log.atInfo()
                    .addKeyValue("cotacaoId", cotacaoId)
                    .addKeyValue("moeda", cotacao.getMoedaPagamento())
                    .addKeyValue("valorPagamento", nova.getValorPagamento())
                    .log("Liquidação registrada");
            return ResultadoLiquidacao.nova(nova);
        }catch (DataIntegrityViolationException concorrenteDaMesmaCotacao){
            Liquidacao vencedora = liquidacaoRepository.findByCotacaoId(cotacaoId)
                    .orElseThrow(() -> concorrenteDaMesmaCotacao);
            contar(cotacao, "existente");
            return ResultadoLiquidacao.existente(vencedora);
        }catch (ObjectOptimisticLockingFailureException outraCotacaoVenceu){
            contar(cotacao, "conflito");
            throw new RecebivelIndisponivelException("O recebível foi liquidado por outra cotação");
        }catch (RecebivelIndisponivelException jaLiquidado){
            contar(cotacao, "conflito");
            throw jaLiquidado;
        }
    }

    private void contar(Cotacao cotacao, String resultado) {
        metricas.counter(METRICA_LIQUIDACOES,
                "moeda", cotacao.getMoedaPagamento().name(),
                "resultado", resultado).increment();
    }

    private Liquidacao efetivar(UUID cotacaoId, String usuario){
        Cotacao cotacao = cotacaoRepository.findById(cotacaoId).orElseThrow();
        Liquidacao liquidacao = liquidacaoRepository.saveAndFlush(Liquidacao.de(cotacao, usuario, clock.instant()));

        Recebivel recebivel = cotacao.getRecebivel();
        recebivel.marcarComoLiquidado();
        recebivelRepository.saveAndFlush(recebivel);

        return liquidacao;
    }
}
