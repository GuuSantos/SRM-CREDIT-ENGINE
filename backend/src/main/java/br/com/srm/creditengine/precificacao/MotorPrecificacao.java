package br.com.srm.creditengine.precificacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MotorPrecificacao {

    private static final int CASAS_MONETARIAS = 2;

    private final Map<TipoRecebivel, EstrategiaPrecificacao> estrategias;

    public MotorPrecificacao(List<EstrategiaPrecificacao> estrategias) {
        this.estrategias = estrategias.stream()
                .collect(Collectors.toUnmodifiableMap(EstrategiaPrecificacao::tipo, Function.identity()));
    }

    public ResultadoPrecificacao precificar(EntradaPrecificacao entrada) {
        EstrategiaPrecificacao estrategia = estrategias.get(entrada.tipo());
        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de recebível sem estratégia: " + entrada.tipo());
        }

        BigDecimal valorPresenteBrl = arredondar(estrategia.calcularValorPresente(
                entrada.valorFace(), entrada.taxaBaseMensal(), entrada.prazoMeses()));
        BigDecimal desagio = entrada.valorFace().subtract(valorPresenteBrl);
        BigDecimal valorPagamento = converter(valorPresenteBrl, entrada);

        return new ResultadoPrecificacao(valorPresenteBrl, desagio, entrada.moedaPagamento(), valorPagamento);
    }

    private BigDecimal converter(BigDecimal valorBrl, EntradaPrecificacao entrada) {
        return switch (entrada.moedaPagamento()) {
            case BRL -> valorBrl;
            case USD -> {
                if (entrada.cambioBrlPorUsd() == null) {
                    throw new IllegalArgumentException("Pagamento em USD exige taxa de câmbio");
                }
                yield arredondar(valorBrl.divide(entrada.cambioBrlPorUsd(), DescontoComposto.PRECISAO));
            }
        };
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_MONETARIAS, RoundingMode.HALF_EVEN);
    }
}