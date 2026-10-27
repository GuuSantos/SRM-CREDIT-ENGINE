package br.com.srm.creditengine.taxas;


import br.com.srm.creditengine.precificacao.Moeda;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/taxas")
public class TaxaController {

    private final TaxaService taxaService;

    public TaxaController(TaxaService taxaService) {
        this.taxaService = taxaService;
    }

    @GetMapping("/vigentes")
    public TaxasVigentesResponse vigentes(){
        TaxaCambio cambio = taxaService.cambioVigente(Moeda.USD);
        return new TaxasVigentesResponse(TaxaBaseResponse.de(taxaService.taxaBaseVigente()), CambioResponse.de(cambio, taxaService.cambioDesatualizado(cambio)));
    }

    @PostMapping("/base")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxaBaseResponse cadastrarTaxaBase(@Valid @RequestBody CadastroTaxaBaseRequest request, Principal usuario){
        return TaxaBaseResponse.de(taxaService.cadastrarTaxaBase(request.taxaMensal(), usuario.getName()));
    }

    @PostMapping("/cambio")
    @ResponseStatus(HttpStatus.CREATED)
    public CambioResponse cadastrarCambio(@Valid @RequestBody CadastroCambioRequest request, Principal usuario){
        TaxaCambio cambio = taxaService.cadastrarCambio(request.brlPorUnidade(), FonteCambio.MANUAL, usuario.getName());
        return CambioResponse.de(cambio, false);
    }
}
