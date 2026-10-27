package br.com.srm.creditengine.liquidacao;

import br.com.srm.creditengine.cotacao.Cotacao;
import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.TipoRecebivel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "liquidacao")
public class Liquidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID cotacaoId;
    private Long recebivelId;
    private Long cedenteId;

    @Enumerated(EnumType.STRING)
    private TipoRecebivel tipo;

    private BigDecimal valorFace;
    private BigDecimal valorPresenteBrl;
    private BigDecimal desagio;

    @Enumerated(EnumType.STRING)
    private Moeda moedaPagamento;

    private BigDecimal valorPagamento;
    private BigDecimal cambioBrlPorUsd;
    private BigDecimal taxaBaseMensal;
    private BigDecimal spreadMensal;
    private Integer prazoDias;
    private String liquidadoPor;
    private Instant liquidadoEm;

    protected Liquidacao() {
    }

    public static Liquidacao de(Cotacao cotacao, String usuario, Instant agora) {
        var recebivel = cotacao.getRecebivel();
        var liquidacao = new Liquidacao();
        liquidacao.cotacaoId = cotacao.getId();
        liquidacao.recebivelId = recebivel.getId();
        liquidacao.cedenteId = recebivel.getCedente().getId();
        liquidacao.tipo = recebivel.getTipo();
        liquidacao.valorFace = recebivel.getValorFace();
        liquidacao.valorPresenteBrl = cotacao.getValorPresenteBrl();
        liquidacao.desagio = cotacao.getDesagio();
        liquidacao.moedaPagamento = cotacao.getMoedaPagamento();
        liquidacao.valorPagamento = cotacao.getValorPagamento();
        liquidacao.cambioBrlPorUsd = cotacao.getCambioBrlPorUsd();
        liquidacao.taxaBaseMensal = cotacao.getTaxaBaseMensal();
        liquidacao.spreadMensal = cotacao.getSpreadMensal();
        liquidacao.prazoDias = cotacao.getPrazoDias();
        liquidacao.liquidadoPor = usuario;
        liquidacao.liquidadoEm = agora;
        return liquidacao;
    }

    public Long getId() { return id; }
    public UUID getCotacaoId() { return cotacaoId; }
    public Long getRecebivelId() { return recebivelId; }
    public Long getCedenteId() { return cedenteId; }
    public TipoRecebivel getTipo() { return tipo; }
    public BigDecimal getValorFace() { return valorFace; }
    public BigDecimal getValorPresenteBrl() { return valorPresenteBrl; }
    public BigDecimal getDesagio() { return desagio; }
    public Moeda getMoedaPagamento() { return moedaPagamento; }
    public BigDecimal getValorPagamento() { return valorPagamento; }
    public BigDecimal getCambioBrlPorUsd() { return cambioBrlPorUsd; }
    public BigDecimal getTaxaBaseMensal() { return taxaBaseMensal; }
    public BigDecimal getSpreadMensal() { return spreadMensal; }
    public Integer getPrazoDias() { return prazoDias; }
    public String getLiquidadoPor() { return liquidadoPor; }
    public Instant getLiquidadoEm() { return liquidadoEm; }
}