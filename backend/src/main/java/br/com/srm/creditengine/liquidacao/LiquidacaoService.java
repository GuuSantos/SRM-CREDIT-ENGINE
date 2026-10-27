package br.com.srm.creditengine.liquidacao;


import br.com.srm.creditengine.cotacao.*;
import br.com.srm.creditengine.recebivel.Recebivel;
import br.com.srm.creditengine.recebivel.RecebivelIndisponivelException;
import br.com.srm.creditengine.recebivel.RecebivelRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class LiquidacaoService {

    private final CotacaoRepository cotacaoRepository;
    private final RecebivelRepository recebivelRepository;
    private final LiquidacaoRepository liquidacaoRepository;
    private final TransactionTemplate transacao;
    private final Clock clock;

    public LiquidacaoService(CotacaoRepository cotacaoRepository, RecebivelRepository recebivelRepository,
                             LiquidacaoRepository liquidacaoRepository, TransactionTemplate transacao, Clock clock) {
        this.cotacaoRepository = cotacaoRepository;
        this.recebivelRepository = recebivelRepository;
        this.liquidacaoRepository = liquidacaoRepository;
        this.transacao = transacao;
        this.clock = clock;
    }

    public ResultadoLiquidacao liquidar(UUID cotacaoId, String usuario){
        Cotacao cotacao = cotacaoRepository.findById(cotacaoId)
                .orElseThrow(() -> new CotacaoNaoEncontradaException("Cotação " + cotacaoId + " não encontrada"));

        if(!cotacao.pertenceA(usuario)){
            throw new CotacaoDeOutroUsuarioException("A cotação " + cotacaoId + " pertence a outro operador");
        }

        Optional<Liquidacao> existente = liquidacaoRepository.findByCotacaoId(cotacaoId);
        if(existente.isPresent()){
            return ResultadoLiquidacao.existente(existente.get());
        }
        if(cotacao.expirada(clock.instant())){
            throw new CotacaoExpiradaException("A Cotação: "+ cotacaoId +"expirou; cote novamente!");
        }
        try {
            return ResultadoLiquidacao.nova(transacao.execute(status -> efetivar(cotacaoId, usuario)));
        }catch (DataIntegrityViolationException concorrenteDaMesmaCotacao){
            return liquidacaoRepository.findByCotacaoId(cotacaoId)
                    .map(ResultadoLiquidacao::existente)
                    .orElseThrow(() -> concorrenteDaMesmaCotacao);
        }catch (ObjectOptimisticLockingFailureException outraCotacaoVenceu){
            throw new RecebivelIndisponivelException("O recebível foi liquidado por outra cotação");
        }
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
