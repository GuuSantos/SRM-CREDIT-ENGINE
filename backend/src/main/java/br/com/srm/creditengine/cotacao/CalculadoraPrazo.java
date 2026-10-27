package br.com.srm.creditengine.cotacao;


import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class CalculadoraPrazo {

    static final ZoneId FUSO_OPERACAO = ZoneId.of("America/Sao_Paulo");
    static final int PRAZO_MAXIMO_DIAS = 5 * 365 + 1;
    private static final BigDecimal DIAS_POR_MES = new BigDecimal("30");

    private final Clock clock;

    public CalculadoraPrazo(Clock clock) {
        this.clock = clock;
    }

    public int diasAte(LocalDate vencimento){
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO_OPERACAO));
        long dias = ChronoUnit.DAYS.between(hoje, vencimento);

        if(dias <= 0 ){
            throw new VencimentoInvalidoException(("O vencimento deve ser posterior a hoje!"));
        }
        if(dias > PRAZO_MAXIMO_DIAS){
            throw new VencimentoInvalidoException(("O Vencimento deve estar em até 5 anos"));
        }
        return (int) dias;
    }

    public static BigDecimal emMeses(int dias){
        return new BigDecimal(dias).divide(DIAS_POR_MES, MathContext.DECIMAL128);
    }
}
