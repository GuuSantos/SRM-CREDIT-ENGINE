package br.com.srm.creditengine.recebivel;


import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cedentes")
public class CedenteController {

    private final CedenteRepository repository;
    public CedenteController(CedenteRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CedenteResponse> listar() {
        return repository.findAll(Sort.by("razaoSocial")).stream()
                .map(CedenteResponse::de)
                .toList();
    }
}
