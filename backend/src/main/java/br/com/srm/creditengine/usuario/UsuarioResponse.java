package br.com.srm.creditengine.usuario;

import java.util.List;

public record UsuarioResponse(
        String usuario,
        List<String> perfis
) {
}
