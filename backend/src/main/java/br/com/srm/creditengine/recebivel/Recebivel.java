package br.com.srm.creditengine.recebivel;


import br.com.srm.creditengine.precificacao.TipoRecebivel;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recebivel")
public class Recebivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cedente_id")
    private Cedente cedente;

    @Enumerated(EnumType.STRING)
    private TipoRecebivel tipo;

    private String numero;
    private BigDecimal valorFace;
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    private StatusRecebivel status;

    @Version
    private Long version;

    private String criadoPor;
    private Instant criadoEm;

    protected Recebivel() {}

    public Recebivel(Cedente cedente, TipoRecebivel tipo, String numero, BigDecimal valorFace,
                     LocalDate vencimento, String criadoPor, Instant criadoEm) {
        this.cedente = cedente;
        this.tipo = tipo;
        this.numero = numero;
        this.valorFace = valorFace;
        this.vencimento = vencimento;
        this.status = StatusRecebivel.PENDENTE;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public boolean estaPendente() {
        return status == StatusRecebivel.PENDENTE;
    }

    public void marcarComoLiquidado(){
        if(!estaPendente()){
            throw new RecebivelIndisponivelException("O recebível " + id + " já foi liquidado");
        }
        this.status = StatusRecebivel.LIQUIDADO;
    }

    public Long getId() {
        return id;
    }

    public Cedente getCedente() {
        return cedente;
    }

    public TipoRecebivel getTipo() {
        return tipo;
    }

    public String getNumero() {
        return numero;
    }

    public BigDecimal getValorFace() {
        return valorFace;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public StatusRecebivel getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
