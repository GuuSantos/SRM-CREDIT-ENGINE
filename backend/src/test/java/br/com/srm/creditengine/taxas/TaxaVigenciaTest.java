package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TaxaVigenciaTest {

    @Autowired
    private TaxaBaseRepository repository;

    @Test
    void retornaATaxaMaisRecenteJaVigenteEIgnoraAsFuturas() {
        Instant agora = Instant.now();
        repository.save(new TaxaBase(new BigDecimal("0.0090"), agora.minus(10, ChronoUnit.DAYS), "teste", agora));
        repository.save(new TaxaBase(new BigDecimal("0.0095"), agora.minus(1, ChronoUnit.DAYS), "teste", agora));
        repository.save(new TaxaBase(new BigDecimal("0.0200"), agora.plus(1, ChronoUnit.DAYS), "teste", agora));

        var vigente = repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                agora.minus(12, ChronoUnit.HOURS));

        assertThat(vigente).isPresent();
        assertThat(vigente.get().getTaxaMensal()).isEqualByComparingTo(new BigDecimal("0.0095"));
    }
}