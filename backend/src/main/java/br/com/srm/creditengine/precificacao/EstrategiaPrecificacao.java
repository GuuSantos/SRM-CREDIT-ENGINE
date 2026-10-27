package br.com.srm.creditengine.precificacao;

import java.math.BigDecimal;

public interface EstrategiaPrecificacao {

    TipoRecebivel tipo();

    BigDecimal spreadMensal();

    BigDecimal calcularValorPresente(BigDecimal valorFace, BigDecimal taxaBaseMensal, BigDecimal prazoMeses);
}
