package br.com.srm.creditengine.extrato;

import br.com.srm.creditengine.TestcontainersConfiguration;
import br.com.srm.creditengine.precificacao.Moeda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// + imports de AutoConfigureMockMvc e httpBasic iguais aos do ApiHttpTest
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ExtratoApiTest {

    private static final RequestPostProcessor OPERADOR = httpBasic("operador1", "senha-operador-teste");
    private static final RequestPostProcessor TESOURARIA = httpBasic("tesouraria1", "senha-tesouraria-teste");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private String cnpj;
    private Long cedenteId;
    private Long taxaCambioId;

    @BeforeEach
    void prepararCedenteIsolado() {
        cnpj = String.format("%014d", ThreadLocalRandom.current().nextLong(100_000_000_000_000L));
        cedenteId = jdbc.queryForObject(
                "INSERT INTO cedente (cnpj, razao_social) VALUES (?, 'Cedente do Extrato') RETURNING id",
                Long.class, cnpj);
        taxaCambioId = jdbc.queryForObject("""
                INSERT INTO taxa_cambio (moeda, brl_por_unidade, vigente_desde, fonte, criado_por, criado_em)
                VALUES ('USD', 5.00000000, now(), 'MANUAL', 'tesouraria1', now()) RETURNING id
                """, Long.class);
    }

    @Test
    void filtraPorPeriodoCedenteEMoeda() throws Exception {
        registrarLiquidacao("EXT-1", Moeda.BRL, "1000.00", "2026-10-09T15:00:00Z");
        registrarLiquidacao("EXT-2", Moeda.USD, "200.00", "2026-10-09T16:00:00Z");
        registrarLiquidacao("EXT-3", Moeda.BRL, "3000.00", "2026-10-12T15:00:00Z");

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-09").param("dataFim", "2026-10-09")
                        .param("cedenteCnpj", cnpj))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2));

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-09").param("dataFim", "2026-10-09")
                        .param("cedenteCnpj", cnpj).param("moeda", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].numeroTitulo").value("EXT-2"))
                .andExpect(jsonPath("$.itens[0].moedaPagamento").value("USD"))
                .andExpect(jsonPath("$.itens[0].valorPagamento").value("200.00"))
                .andExpect(jsonPath("$.itens[0].cedenteRazaoSocial").value("Cedente do Extrato"));
    }

    @Test
    void periodoConsideraODiaEmSaoPaulo() throws Exception {
        // 02:30 UTC do dia 10 = 23:30 do dia 09 em São Paulo
        registrarLiquidacao("EXT-FUSO", Moeda.BRL, "500.00", "2026-10-10T02:30:00Z");

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-09").param("dataFim", "2026-10-09")
                        .param("cedenteCnpj", cnpj))
                .andExpect(jsonPath("$.totalItens").value(1));

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-10").param("dataFim", "2026-10-10")
                        .param("cedenteCnpj", cnpj))
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test
    void paginaEOrdenaPorColunaPermitida() throws Exception {
        registrarLiquidacao("EXT-A", Moeda.BRL, "100.00", "2026-10-09T15:00:00Z");
        registrarLiquidacao("EXT-B", Moeda.BRL, "300.00", "2026-10-09T15:01:00Z");
        registrarLiquidacao("EXT-C", Moeda.BRL, "200.00", "2026-10-09T15:02:00Z");

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-09").param("dataFim", "2026-10-09")
                        .param("cedenteCnpj", cnpj)
                        .param("ordenarPor", "VALOR_PAGAMENTO").param("direcao", "ASC")
                        .param("tamanho", "2").param("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].valorPagamento").value("100.00"))
                .andExpect(jsonPath("$.itens[1].valorPagamento").value("200.00"));

        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-09").param("dataFim", "2026-10-09")
                        .param("cedenteCnpj", cnpj)
                        .param("ordenarPor", "VALOR_PAGAMENTO").param("direcao", "ASC")
                        .param("tamanho", "2").param("pagina", "1"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].valorPagamento").value("300.00"));
    }

    @Test
    void tamanhoAcimaDe100Retorna400() throws Exception {
        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-01").param("dataFim", "2026-10-31")
                        .param("tamanho", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("tamanho"));
    }

    @Test
    void sqlInjectionNoCnpjRetorna400() throws Exception {
        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-01").param("dataFim", "2026-10-31")
                        .param("cedenteCnpj", "' OR 1=1--"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cedenteCnpj"));
    }

    @Test
    void periodoInvertidoRetorna400() throws Exception {
        mvc.perform(get("/liquidacoes").with(OPERADOR)
                        .param("dataInicio", "2026-10-31").param("dataFim", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("periodoValido"));
    }

    @Test
    void tesourariaNaoConsultaExtrato() throws Exception {
        mvc.perform(get("/liquidacoes").with(TESOURARIA)
                        .param("dataInicio", "2026-10-01").param("dataFim", "2026-10-31"))
                .andExpect(status().isForbidden());
    }

    // Monta recebível → cotação → liquidação direto no banco para controlar o horário exato.
    // Os valores não precisam bater com o motor: aqui o alvo é a consulta, não o cálculo.
    private void registrarLiquidacao(String numero, Moeda moeda, String valorPagamento, String liquidadoEm) {
        OffsetDateTime quando = Instant.parse(liquidadoEm).atOffset(ZoneOffset.UTC);
        boolean usd = moeda == Moeda.USD;
        BigDecimal cambio = usd ? new BigDecimal("5.00000000") : null;

        Long recebivelId = jdbc.queryForObject("""
                INSERT INTO recebivel (cedente_id, tipo, numero, valor_face, vencimento, status, criado_por, criado_em)
                VALUES (?, 'DUPLICATA_MERCANTIL', ?, 100000.00, DATE '2027-12-31', 'LIQUIDADO', 'operador1', ?)
                RETURNING id
                """, Long.class, cedenteId, numero, quando);

        UUID cotacaoId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cotacao (id, recebivel_id, taxa_base_id, taxa_cambio_id, taxa_base_mensal, spread_mensal,
                                     prazo_dias, moeda_pagamento, cambio_brl_por_usd, valor_presente_brl, desagio,
                                     valor_pagamento, expira_em, criado_por, criado_em)
                VALUES (?, ?, (SELECT MIN(id) FROM taxa_base), ?, 0.01, 0.015, 90, ?, ?, 92859.94, 7140.06,
                        ?, ?, 'operador1', ?)
                """, cotacaoId, recebivelId, usd ? taxaCambioId : null, moeda.name(), cambio,
                new BigDecimal(valorPagamento), quando.plusMinutes(2), quando);

        jdbc.update("""
                INSERT INTO liquidacao (cotacao_id, recebivel_id, cedente_id, tipo, valor_face, valor_presente_brl,
                                        desagio, moeda_pagamento, valor_pagamento, cambio_brl_por_usd,
                                        taxa_base_mensal, spread_mensal, prazo_dias, liquidado_por, liquidado_em)
                VALUES (?, ?, ?, 'DUPLICATA_MERCANTIL', 100000.00, 92859.94, 7140.06, ?, ?, ?,
                        0.01, 0.015, 90, 'operador1', ?)
                """, cotacaoId, recebivelId, cedenteId, moeda.name(), new BigDecimal(valorPagamento), cambio, quando);
    }
}