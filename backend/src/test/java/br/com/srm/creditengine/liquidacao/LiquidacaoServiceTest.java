package br.com.srm.creditengine.liquidacao;

import br.com.srm.creditengine.RelogioControlado;
import br.com.srm.creditengine.RelogioTestConfig;
import br.com.srm.creditengine.TestcontainersConfiguration;
import br.com.srm.creditengine.cotacao.Cotacao;
import br.com.srm.creditengine.cotacao.CotacaoDeOutroUsuarioException;
import br.com.srm.creditengine.cotacao.CotacaoExpiradaException;
import br.com.srm.creditengine.cotacao.CotacaoService;
import br.com.srm.creditengine.cotacao.DadosRecebivelRequest;
import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import br.com.srm.creditengine.recebivel.RecebivelIndisponivelException;
import br.com.srm.creditengine.recebivel.RecebivelRepository;
import br.com.srm.creditengine.recebivel.StatusRecebivel;
import br.com.srm.creditengine.taxas.FonteCambio;
import br.com.srm.creditengine.taxas.TaxaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RelogioTestConfig.class})
class LiquidacaoServiceTest {

    private static final String OPERADOR = "operador1";

    @Autowired private LiquidacaoService liquidacaoService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private TaxaService taxaService;
    @Autowired private LiquidacaoRepository liquidacaoRepository;
    @Autowired private RecebivelRepository recebivelRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RelogioControlado relogio;

    @AfterEach
    void reiniciarRelogio() {
        relogio.reiniciar();
    }

    @Test
    void liquidaUmaVezERepeticaoDevolveAMesmaLiquidacao() {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);

        ResultadoLiquidacao primeira = liquidacaoService.liquidar(cotacao.getId(), OPERADOR);
        ResultadoLiquidacao repeticao = liquidacaoService.liquidar(cotacao.getId(), OPERADOR);

        assertThat(primeira.criada()).isTrue();
        assertThat(repeticao.criada()).isFalse();
        assertThat(repeticao.liquidacao().getId()).isEqualTo(primeira.liquidacao().getId());
        assertThat(recebivelRepository.findById(cotacao.getRecebivel().getId()).orElseThrow().getStatus())
                .isEqualTo(StatusRecebivel.LIQUIDADO);
    }

    @Test
    void dezLiquidacoesSimultaneasDaMesmaCotacaoGeramUmUnicoRegistro() throws Exception {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);

        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tarefas.add(() -> liquidacaoService.liquidar(cotacao.getId(), OPERADOR));
        }
        List<Object> resultados = executarAoMesmoTempo(tarefas);

        assertThat(resultados).allMatch(ResultadoLiquidacao.class::isInstance);
        long criadas = resultados.stream().map(ResultadoLiquidacao.class::cast).filter(ResultadoLiquidacao::criada).count();
        assertThat(criadas).isEqualTo(1);
        assertThat(resultados.stream().map(r -> ((ResultadoLiquidacao) r).liquidacao().getId()).distinct()).hasSize(1);
    }

    @Test
    void duasCotacoesDoMesmoRecebivelAoMesmoTempoUmaVenceEAOutraRecebeConflito() throws Exception {
        String numero = novoNumero();
        Cotacao cotacaoA = cotar(numero, Moeda.BRL);
        Cotacao cotacaoB = cotar(numero, Moeda.BRL);

        List<Object> resultados = executarAoMesmoTempo(List.of(
                () -> liquidacaoService.liquidar(cotacaoA.getId(), OPERADOR),
                () -> liquidacaoService.liquidar(cotacaoB.getId(), OPERADOR)));

        assertThat(resultados).filteredOn(ResultadoLiquidacao.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(RecebivelIndisponivelException.class::isInstance).hasSize(1);

        boolean aVenceu = liquidacaoRepository.findByCotacaoId(cotacaoA.getId()).isPresent();
        UUID perdedora = aVenceu ? cotacaoB.getId() : cotacaoA.getId();
        assertThat(liquidacaoRepository.findByCotacaoId(perdedora)).isEmpty();
    }

    @Test
    void cotacaoExpiradaNaoLiquidaENadaEhGravado() {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);
        relogio.avancar(Duration.ofSeconds(121));

        assertThatThrownBy(() -> liquidacaoService.liquidar(cotacao.getId(), OPERADOR))
                .isInstanceOf(CotacaoExpiradaException.class);
        assertThat(liquidacaoRepository.findByCotacaoId(cotacao.getId())).isEmpty();
    }

    @Test
    void repeticaoDeUmaLiquidacaoJaFeitaDevolveAMesmaMesmoDepoisDeExpirar() {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);
        ResultadoLiquidacao original = liquidacaoService.liquidar(cotacao.getId(), OPERADOR);
        relogio.avancar(Duration.ofMinutes(3));

        ResultadoLiquidacao repeticao = liquidacaoService.liquidar(cotacao.getId(), OPERADOR);

        assertThat(repeticao.criada()).isFalse();
        assertThat(repeticao.liquidacao().getId()).isEqualTo(original.liquidacao().getId());
    }

    @Test
    void outroOperadorNaoLiquidaACotacao() {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);

        assertThatThrownBy(() -> liquidacaoService.liquidar(cotacao.getId(), "operador2"))
                .isInstanceOf(CotacaoDeOutroUsuarioException.class);
    }

    @Test
    void liquidacaoUsaOCambioCongeladoNaCotacao() {
        taxaService.cadastrarCambio(new BigDecimal("5.4321"), FonteCambio.MANUAL, "teste");
        Cotacao cotacao = cotar(novoNumero(), Moeda.USD);
        taxaService.cadastrarCambio(new BigDecimal("6.0000"), FonteCambio.MANUAL, "teste");

        Liquidacao liquidacao = liquidacaoService.liquidar(cotacao.getId(), OPERADOR).liquidacao();

        assertThat(liquidacao.getValorPagamento()).isEqualTo(new BigDecimal("17094.67"));
        assertThat(liquidacao.getCambioBrlPorUsd()).isEqualByComparingTo(new BigDecimal("5.4321"));
    }

    @Test
    void liquidacaoNaoPodeSerAlteradaNemApagadaNemDiretoNoBanco() {
        Cotacao cotacao = cotar(novoNumero(), Moeda.BRL);
        Long id = liquidacaoService.liquidar(cotacao.getId(), OPERADOR).liquidacao().getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE liquidacao SET valor_pagamento = 1 WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutáveis");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM liquidacao WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutáveis");
    }

    private Cotacao cotar(String numero, Moeda moeda) {
        LocalDate hoje = LocalDate.now(relogio.withZone(ZoneId.of("America/Sao_Paulo")));
        var dados = new DadosRecebivelRequest(1L, TipoRecebivel.DUPLICATA_MERCANTIL, numero,
                new BigDecimal("100000.00"), hoje.plusDays(90), moeda);
        return cotacaoService.cotar(dados, OPERADOR);
    }

    private static String novoNumero() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static List<Object> executarAoMesmoTempo(List<Callable<Object>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    try {
                        return tarefa.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            largada.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> futuro : futuros) {
                resultados.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }
}