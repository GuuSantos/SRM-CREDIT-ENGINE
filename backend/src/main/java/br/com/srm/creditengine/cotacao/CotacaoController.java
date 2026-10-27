package br.com.srm.creditengine.cotacao;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class CotacaoController {

    private final CotacaoService cotacaoService;

    public  CotacaoController(CotacaoService cotacaoService) {
        this.cotacaoService = cotacaoService;
    }

    @PostMapping("/simulacoes")
    public SimulacaoResponse simular(@Valid @RequestBody DadosRecebivelRequest dados){
        return SimulacaoResponse.de(cotacaoService.simular(dados));
    }

    @PostMapping("/cotacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public CotacaoResponse cotar(@Valid @RequestBody DadosRecebivelRequest dados, Principal usuario){
        return CotacaoResponse.de(cotacaoService.cotar(dados, usuario.getName()));
    }
}
