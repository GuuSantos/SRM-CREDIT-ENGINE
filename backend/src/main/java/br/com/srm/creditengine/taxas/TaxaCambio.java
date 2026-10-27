package br.com.srm.creditengine.taxas;


import br.com.srm.creditengine.precificacao.Moeda;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "taxa_cambio")
public class TaxaCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Moeda moeda;

    private BigDecimal brlPorUnidade;
    private Instant vigenteDesde;

    @Enumerated(EnumType.STRING)
    private FonteCambio fonte;

    private String criadoPor;
    private Instant criadoEm;

    protected TaxaCambio() {
    }

    public TaxaCambio(Moeda moeda, BigDecimal brlPorUnidade, Instant vigenteDesde, FonteCambio fonte, String criadoPor, Instant criadoEm) {
        this.moeda = moeda;
        this.brlPorUnidade = brlPorUnidade;
        this.vigenteDesde = vigenteDesde;
        this.fonte = fonte;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public BigDecimal getBrlPorUnidade() {
        return brlPorUnidade;
    }

    public Instant getVigenteDesde() {
        return vigenteDesde;
    }

    public FonteCambio getFonte() {
        return fonte;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
