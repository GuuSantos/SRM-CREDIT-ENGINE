package br.com.srm.creditengine;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@TestConfiguration(proxyBeanMethods = false)
public class RelogioTestConfig {

    @Bean
    @Primary
    public RelogioControlado relogioControlado() {
        return new RelogioControlado(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}