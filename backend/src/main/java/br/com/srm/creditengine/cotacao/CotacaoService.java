package br.com.srm.creditengine.cotacao;


import br.com.srm.creditengine.precificacao.EntradaPrecificacao;
import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.MotorPrecificacao;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import br.com.srm.creditengine.recebivel.*;
import br.com.srm.creditengine.taxas.CambioDesatualizadoException;
import br.com.srm.creditengine.taxas.TaxaBase;
import br.com.srm.creditengine.taxas.TaxaCambio;
import br.com.srm.creditengine.taxas.TaxaService;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CotacaoService {
    static final Duration VALIDADE = Duration.ofMinutes(2);

    private final MotorPrecificacao motor;
    private final TaxaService taxaService;;
    private final CalculadoraPrazo calculadoraPrazo;
    private final CedenteRepository cedenteRepository;
    private final RecebivelRepository recebivelRepository;
    private final CotacaoRepository cotacaoRepository;
    private final Clock clock;
    private final Timer tempoMotor;


    public CotacaoService(MotorPrecificacao motor, TaxaService taxaService, CalculadoraPrazo calculadoraPrazo,
                          CedenteRepository cedenteRepository, RecebivelRepository recebivelRepository,
                          CotacaoRepository cotacaoRepository, Clock clock, MeterRegistry metricas) {
        this.motor = motor;
        this.taxaService = taxaService;
        this.calculadoraPrazo = calculadoraPrazo;
        this.cedenteRepository = cedenteRepository;
        this.recebivelRepository = recebivelRepository;
        this.cotacaoRepository = cotacaoRepository;
        this.clock = clock;
        this.tempoMotor = Timer.builder("precificacao.motor")
                .description("Tempo de cálculo do motor de precificação")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(metricas);
    }

    public PrecificacaoCalculada simular(DadosRecebivelRequest dados){
       buscarCedente(dados.cedenteId());
       return precificar(dados.tipo(), dados.valorFace(), dados.vencimento(), dados.moedaPagamento());
    }

    public Cotacao cotar(DadosRecebivelRequest dados, String usuario){
        Cedente cedente = buscarCedente(dados.cedenteId());
        Recebivel recebivel = obterOuCadastrarRecebivel(cedente, dados, usuario);

        PrecificacaoCalculada calculo = precificar(recebivel.getTipo(), recebivel.getValorFace(), recebivel.getVencimento(), dados.moedaPagamento());
        if(calculo.cambioDesatualizado()){
            throw new CambioDesatualizadoException("A taxa de câmbio vigente tem mais de 24h; acione a Tesouraria!");
        }

        Instant agora = clock.instant();
        TaxaCambio cambio = calculo.cambio();
        Cotacao cotacao = new Cotacao(
                recebivel,
                calculo.taxaBase().getId(),
                cambio == null ? null : cambio.getId(),
                calculo.taxaBase().getTaxaMensal(),
                calculo.spreadMensal(),
                calculo.prazoDias(),
                cambio == null ? null : cambio.getBrlPorUnidade(),
                calculo.resultado(),
                agora.plus(VALIDADE),
                usuario,
                agora);
        return cotacaoRepository.save(cotacao);
    }

    private PrecificacaoCalculada precificar(TipoRecebivel tipo, BigDecimal valorFace, LocalDate vencimento, Moeda moeda){
        int prazoDias = calculadoraPrazo.diasAte(vencimento);
        TaxaBase taxaBase = taxaService.taxaBaseVigente();
        TaxaCambio cambio = moeda == Moeda.USD ? taxaService.cambioVigente(Moeda.USD) : null;

        var entrada = new EntradaPrecificacao(tipo, valorFace, CalculadoraPrazo.emMeses(prazoDias), taxaBase.getTaxaMensal(), moeda, cambio == null? null : cambio.getBrlPorUnidade());

        boolean desatualizado = cambio != null && taxaService.cambioDesatualizado(cambio);
        var resultado = tempoMotor.record(() -> motor.precificar(entrada));
        return new PrecificacaoCalculada(prazoDias, taxaBase, motor.spreadMensal(tipo), cambio, desatualizado, resultado);
    }

    private Recebivel obterOuCadastrarRecebivel(Cedente cedente, DadosRecebivelRequest dados, String usuario){
        return recebivelRepository.findByCedenteIdAndTipoAndNumero(cedente.getId(), dados.tipo(), dados.numero())
                .map(existente -> validarExistente(existente, dados))
                .orElseGet(() -> recebivelRepository.save(new Recebivel(cedente, dados.tipo(), dados.numero(),
                        dados.valorFace(), dados.vencimento(), usuario, clock.instant())));
    }

    private Recebivel validarExistente(Recebivel existente, DadosRecebivelRequest dados){
        if(!existente.estaPendente()){
            throw new RecebivelIndisponivelException("O Título "+ existente.getNumero() + "já foi liquidado!");
        }
        boolean mesmosDados = existente.getValorFace().compareTo(dados.valorFace()) == 0 && existente.getVencimento().equals(dados.vencimento());
        if(!mesmosDados){
            throw new RecebivelIndisponivelException("O título "+existente.getNumero()+" já está cadastrado com outro valor ou vencimento");
        }
        return existente;
    }

    private Cedente buscarCedente(Long cedenteId){
        return cedenteRepository.findById(cedenteId)
                .orElseThrow(() -> new CedenteNaoEncontradoException("Cedente "+ cedenteId + " não encontrado"));
    }
}
