package br.com.srm.creditengine.liquidacao;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
public class LiquidacaoController {
    private final LiquidacaoService liquidacaoService;

    public LiquidacaoController(LiquidacaoService liquidacaoService) {
        this.liquidacaoService = liquidacaoService;
    }

    @PutMapping("/cotacoes/{cotacaoId}/liquidacao")
    public ResponseEntity<LiquidacaoResponse> liquidar (@PathVariable UUID cotacaoId, Principal usuario){
        ResultadoLiquidacao resultado = liquidacaoService.liquidar(cotacaoId, usuario.getName());
        HttpStatus status = resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(LiquidacaoResponse.de(resultado.liquidacao()));
    }
}
