package br.com.srm.creditengine.taxas;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "taxa_base")
public class TaxaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal taxaMensal;
    private Instant vigenteDesde;
    private String criadoPor;
    private Instant criadoEm;

    protected TaxaBase() {
    }

    public TaxaBase(BigDecimal taxaMensal, Instant vigenteDesde, String criadoPor, Instant criadoEm) {
        this.taxaMensal = taxaMensal;
        this.vigenteDesde = vigenteDesde;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getTaxaMensal() {
        return taxaMensal;
    }

    public Instant getVigenteDesde() {
        return vigenteDesde;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
