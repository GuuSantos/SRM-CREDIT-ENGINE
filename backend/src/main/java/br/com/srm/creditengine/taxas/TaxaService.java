package br.com.srm.creditengine.taxas;


import br.com.srm.creditengine.precificacao.Moeda;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class TaxaService {

    static final Duration IDADE_MAXIMA_CAMBIO = Duration.ofHours(24);

    private final TaxaBaseRepository taxaBaseRepository;
    private final TaxaCambioRepository taxaCambioRepository;
    private final Clock clock;

    public TaxaService(TaxaBaseRepository taxaBaseRepository, TaxaCambioRepository taxaCambioRepository, Clock clock) {
        this.taxaBaseRepository = taxaBaseRepository;
        this.taxaCambioRepository = taxaCambioRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TaxaBase taxaBaseVigente(){
        return taxaBaseRepository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(clock.instant())
                .orElseThrow(() -> new TaxaNaoEncontradaException("Nenhuma taxa base vigente cadastrada"));
    }

    @Transactional(readOnly = true)
    public TaxaCambio cambioVigente(Moeda moeda){
        return taxaCambioRepository.findFirstByMoedaAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(moeda, clock.instant())
                .orElseThrow(() -> new TaxaNaoEncontradaException("Nenhuma taxa de câmbio vigente para "+moeda));
    }

    public boolean cambioDesatualizado(TaxaCambio cambio){
        Duration idade = Duration.between(cambio.getVigenteDesde(), clock.instant());
        return idade.compareTo(IDADE_MAXIMA_CAMBIO) > 0;
    }

    public TaxaBase cadastrarTaxaBase(BigDecimal taxaMensal, String usuario){
        Instant agora = clock.instant();
        return taxaBaseRepository.save(new TaxaBase(taxaMensal, agora, usuario, agora));
    }

    public TaxaCambio cadastrarCambio(BigDecimal brlPorUnidade, FonteCambio fonte, String usuario){
        Instant agora = clock.instant();
        return taxaCambioRepository.save(new TaxaCambio(Moeda.USD, brlPorUnidade, agora, fonte, usuario, agora));
    }
}
