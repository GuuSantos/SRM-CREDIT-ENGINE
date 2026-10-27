package br.com.srm.creditengine.cotacao;

import br.com.srm.creditengine.RelogioControlado;
import br.com.srm.creditengine.RelogioTestConfig;
import br.com.srm.creditengine.TestcontainersConfiguration;
import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import br.com.srm.creditengine.recebivel.RecebivelIndisponivelException;
import br.com.srm.creditengine.taxas.CambioDesatualizadoException;
import br.com.srm.creditengine.taxas.FonteCambio;
import br.com.srm.creditengine.taxas.TaxaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RelogioTestConfig.class})
@Transactional
class CotacaoServiceTest {

    private static final Long CEDENTE_ALFA = 1L;

    @Autowired
    private CotacaoService cotacaoService;

    @Autowired
    private TaxaService taxaService;

    @Autowired
    private RelogioControlado relogio;

    @AfterEach
    void reiniciarRelogio() {
        relogio.reiniciar();
    }

    @Test
    void cotacaoCongelaOsValoresDoGoldenCaseC1() {
        Cotacao cotacao = cotacaoService.cotar(duplicata("DUP-1", new BigDecimal("100000.00"), 90, Moeda.BRL), "operador1");

        assertThat(cotacao.getPrazoDias()).isEqualTo(90);
        assertThat(cotacao.getValorPresenteBrl()).isEqualTo(new BigDecimal("92859.94"));
        assertThat(cotacao.getDesagio()).isEqualTo(new BigDecimal("7140.06"));
        assertThat(cotacao.getSpreadMensal()).isEqualByComparingTo(new BigDecimal("0.015"));
        assertThat(cotacao.getExpiraEm()).isEqualTo(relogio.instant().plus(Duration.ofMinutes(2)));
    }

    @Test
    void cotarDeNovoOMesmoTituloReaproveitaORecebivel() {
        var dados = duplicata("DUP-2", new BigDecimal("5000.00"), 30, Moeda.BRL);

        Cotacao primeira = cotacaoService.cotar(dados, "operador1");
        Cotacao segunda = cotacaoService.cotar(dados, "operador1");

        assertThat(segunda.getRecebivel().getId()).isEqualTo(primeira.getRecebivel().getId());
        assertThat(segunda.getId()).isNotEqualTo(primeira.getId());
    }

    @Test
    void mesmoTituloComOutroValorEhRecusado() {
        cotacaoService.cotar(duplicata("DUP-3", new BigDecimal("5000.00"), 30, Moeda.BRL), "operador1");

        var divergente = duplicata("DUP-3", new BigDecimal("9999.00"), 30, Moeda.BRL);

        assertThatThrownBy(() -> cotacaoService.cotar(divergente, "operador1"))
                .isInstanceOf(RecebivelIndisponivelException.class);
    }

    @Test
    void cambioComMaisDe24hBloqueiaACotacaoMasSoAvisaNaSimulacao() {
        taxaService.cadastrarCambio(new BigDecimal("5.4321"), FonteCambio.MANUAL, "teste");
        relogio.avancar(Duration.ofHours(25));

        var dados = duplicata("DUP-4", new BigDecimal("100000.00"), 90, Moeda.USD);

        assertThat(cotacaoService.simular(dados).cambioDesatualizado()).isTrue();
        assertThatThrownBy(() -> cotacaoService.cotar(dados, "operador1"))
                .isInstanceOf(CambioDesatualizadoException.class);
    }

    @Test
    void vencimentoHojeEhRecusado() {
        var dados = duplicata("DUP-5", new BigDecimal("5000.00"), 0, Moeda.BRL);

        assertThatThrownBy(() -> cotacaoService.simular(dados))
                .isInstanceOf(VencimentoInvalidoException.class);
    }

    private DadosRecebivelRequest duplicata(String numero, BigDecimal valorFace, int diasAteVencimento, Moeda moeda) {
        LocalDate hoje = LocalDate.now(relogio.withZone(ZoneId.of("America/Sao_Paulo")));
        return new DadosRecebivelRequest(CEDENTE_ALFA, TipoRecebivel.DUPLICATA_MERCANTIL, numero,
                valorFace, hoje.plusDays(diasAteVencimento), moeda);
    }
}