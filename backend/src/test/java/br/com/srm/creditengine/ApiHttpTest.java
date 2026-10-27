package br.com.srm.creditengine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ApiHttpTest {

    private static final RequestPostProcessor OPERADOR = httpBasic("operador1", "senha-operador-teste");
    private static final RequestPostProcessor TESOURARIA = httpBasic("tesouraria1", "senha-tesouraria-teste");

    @Autowired
    private MockMvc mvc;

    @Test
    void semCredencialRecebe401EmProblemDetailsSemAbrirJanelaDoNavegador() throws Exception {
        mvc.perform(get("/taxas/vigentes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void healthEhPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void operadorNaoCadastraTaxaETesourariaCadastra() throws Exception {
        String corpo = "{\"taxaMensal\": \"0.0110\"}";

        mvc.perform(post("/taxas/base").with(OPERADOR).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mvc.perform(post("/taxas/base").with(TESOURARIA).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxaMensal").value("0.0110"))
                .andExpect(jsonPath("$.criadoPor").value("tesouraria1"));
    }

    @Test
    void tesourariaNaoLiquida() throws Exception {
        mvc.perform(put("/cotacoes/{id}/liquidacao", UUID.randomUUID()).with(TESOURARIA))
                .andExpect(status().isForbidden());
    }

    @Test
    void taxaAcimaDoTetoEhRecusadaComOCampoIndicado() throws Exception {
        mvc.perform(post("/taxas/base").with(TESOURARIA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxaMensal\": \"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("taxaMensal"));
    }

    @Test
    void tipoDeRecebivelDesconhecidoEhRecusado() throws Exception {
        mvc.perform(post("/simulacoes").with(OPERADOR).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRecebivel("BOLETO", "100000.00", "BRL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cotacaoInexistenteDevolve404ComCodigo() throws Exception {
        mvc.perform(put("/cotacoes/{id}/liquidacao", UUID.randomUUID()).with(OPERADOR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("COTACAO_NAO_ENCONTRADA"));
    }

    @Test
    void simulacaoDevolveValoresComoTexto() throws Exception {
        mvc.perform(post("/simulacoes").with(OPERADOR).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRecebivel("DUPLICATA_MERCANTIL", "100000.00", "BRL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prazoDias").value(90))
                .andExpect(jsonPath("$.valorPresenteBrl").value("92859.94"))
                .andExpect(jsonPath("$.desagio").value("7140.06"));
    }

    @Test
    void fluxoCompletoCotarLiquidarERepetir() throws Exception {
        String resposta = mvc.perform(post("/cotacoes").with(OPERADOR).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRecebivel("DUPLICATA_MERCANTIL", "100000.00", "BRL")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cotacaoId = resposta.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(put("/cotacoes/{id}/liquidacao", cotacaoId).with(OPERADOR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorPagamento").value("92859.94"));

        mvc.perform(put("/cotacoes/{id}/liquidacao", cotacaoId).with(OPERADOR))
                .andExpect(status().isOk());

        mvc.perform(put("/cotacoes/{id}/liquidacao", cotacaoId).with(httpBasic("operador2", "senha-operador-teste")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("COTACAO_DE_OUTRO_OPERADOR"));
    }

    @Test
    void usuarioAutenticadoConsultaOProprioPerfil() throws Exception {
        mvc.perform(get("/usuarios/eu").with(httpBasic("tesouraria1", "senha-tesouraria-teste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("tesouraria1"))
                .andExpect(jsonPath("$.perfis[0]").value("TESOURARIA"));
    }

    @Test
    void senhaErradaNoUsuarioAtualRetorna401() throws Exception {
        mvc.perform(get("/usuarios/eu").with(httpBasic("operador1", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    private static String corpoRecebivel(String tipo, String valorFace, String moeda) {
        LocalDate vencimento = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(90);
        return """
                {"cedenteId": 1, "tipo": "%s", "numero": "%s", "valorFace": "%s",
                 "vencimento": "%s", "moedaPagamento": "%s"}
                """.formatted(tipo, "HTTP-" + UUID.randomUUID().toString().substring(0, 8), valorFace, vencimento, moeda);
    }
}