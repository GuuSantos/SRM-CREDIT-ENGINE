package br.com.srm.creditengine;

import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

// + imports de AutoConfigureMockMvc e httpBasic iguais aos do ApiHttpTest
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ObservabilidadeTest {

    private static final RequestPostProcessor OPERADOR = httpBasic("operador1", "senha-operador-teste");

    @Autowired MockMvc mvc;
    @Autowired MeterRegistry metricas;

    @Test
    void todaRespostaTemTraceIdInclusiveSemAutenticacao() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().exists("X-Trace-Id"));
        mvc.perform(get("/cedentes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void liquidacaoIncrementaContadorEMotorRegistraTempo() throws Exception {
        double liquidacoesAntes = contador("BRL", "nova");
        long calculosAntes = execucoesDoMotor();

        String corpo = """
                {"cedenteId": 1, "tipo": "DUPLICATA_MERCANTIL", "numero": "%s", "valorFace": "100000.00",
                 "vencimento": "%s", "moedaPagamento": "BRL"}
                """.formatted("OBS-" + UUID.randomUUID().toString().substring(0, 8),
                LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(90));

        String resposta = mvc.perform(post("/cotacoes").with(OPERADOR)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cotacaoId = JsonPath.read(resposta, "$.id");

        mvc.perform(put("/cotacoes/{id}/liquidacao", cotacaoId).with(OPERADOR))
                .andExpect(status().isCreated());

        assertThat(contador("BRL", "nova")).isEqualTo(liquidacoesAntes + 1);
        assertThat(execucoesDoMotor()).isGreaterThan(calculosAntes);
    }

    private double contador(String moeda, String resultado) {
        Counter contador = metricas.find("liquidacoes").tags("moeda", moeda, "resultado", resultado).counter();
        return contador == null ? 0 : contador.count();
    }

    private long execucoesDoMotor() {
        Timer timer = metricas.find("precificacao.motor").timer();
        return timer == null ? 0 : timer.count();
    }
}