package br.com.srm.creditengine.recebivel;


import jakarta.persistence.*;

@Entity
@Table(name = "cedente")
public class Cedente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cnpj;
    private String razaoSocial;

    protected Cedente() {}

    public Long getId() {
        return id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }
}
