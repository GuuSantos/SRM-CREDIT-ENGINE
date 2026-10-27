package br.com.srm.creditengine.cotacao;


import br.com.srm.creditengine.precificacao.Moeda;
import br.com.srm.creditengine.precificacao.ResultadoPrecificacao;
import br.com.srm.creditengine.recebivel.Recebivel;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cotacao")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recebivel_id")
    private Recebivel recebivel;

    private Long taxaBaseId;
    private Long taxaCambioId;
    private BigDecimal taxaBaseMensal;
    private BigDecimal spreadMensal;
    private Integer prazoDias;

    @Enumerated(EnumType.STRING)
    private Moeda moedaPagamento;

    private BigDecimal cambioBrlPorUsd;
    private BigDecimal valorPresenteBrl;
    private BigDecimal desagio;
    private BigDecimal valorPagamento;
    private Instant expiraEm;
    private String criadoPor;
    private Instant criadoEm;

    protected Cotacao() {}

    public Cotacao(Recebivel recebivel, Long taxaBaseId, Long taxaCambioId, BigDecimal taxaBaseMensal, BigDecimal spreadMensal,
                   Integer prazoDias, BigDecimal cambioBrlPorUsd, ResultadoPrecificacao resultado,
                   Instant expiraEm, String criadoPor, Instant criadoEm) {
        this.recebivel = recebivel;
        this.taxaBaseId = taxaBaseId;
        this.taxaCambioId = taxaCambioId;
        this.taxaBaseMensal = taxaBaseMensal;
        this.spreadMensal = spreadMensal;
        this.prazoDias = prazoDias;
        this.moedaPagamento = resultado.moedaPagamento();
        this.cambioBrlPorUsd = cambioBrlPorUsd;
        this.valorPresenteBrl = resultado.valorPresenteBrl();
        this.desagio = resultado.desagio();
        this.valorPagamento = resultado.valorPagamento();
        this.expiraEm = expiraEm;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public boolean expirada(Instant agora){
        return !agora.isBefore(expiraEm);
    }

    public boolean pertenceA(String usuario){
        return criadoPor.equals(usuario);
    }

    public UUID getId() { return id; }
    public Recebivel getRecebivel() { return recebivel; }
    public Long getTaxaBaseId() { return taxaBaseId; }
    public Long getTaxaCambioId() { return taxaCambioId; }
    public BigDecimal getTaxaBaseMensal() { return taxaBaseMensal; }
    public BigDecimal getSpreadMensal() { return spreadMensal; }
    public Integer getPrazoDias() { return prazoDias; }
    public Moeda getMoedaPagamento() { return moedaPagamento; }
    public BigDecimal getCambioBrlPorUsd() { return cambioBrlPorUsd; }
    public BigDecimal getValorPresenteBrl() { return valorPresenteBrl; }
    public BigDecimal getDesagio() { return desagio; }
    public BigDecimal getValorPagamento() { return valorPagamento; }
    public Instant getExpiraEm() { return expiraEm; }
    public String getCriadoPor() { return criadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
}
