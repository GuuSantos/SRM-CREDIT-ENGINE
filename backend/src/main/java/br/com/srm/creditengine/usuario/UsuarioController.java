package br.com.srm.creditengine.usuario;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final String PREFIXO_PAPEL = "ROLE_";

    @GetMapping("/eu")
    public UsuarioResponse eu(Authentication authentication) {
        List<String> perfis = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(PREFIXO_PAPEL))
                .map(papel -> papel.substring(PREFIXO_PAPEL.length()))
                .sorted()
                .toList();
        return new UsuarioResponse(authentication.getName(), perfis);
    }
}
