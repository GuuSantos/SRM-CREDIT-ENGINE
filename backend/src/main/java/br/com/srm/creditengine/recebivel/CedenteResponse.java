package br.com.srm.creditengine.recebivel;

public record CedenteResponse(
        Long id,
        String cnpj,
        String razaoSocial
) {
    static CedenteResponse de(Cedente cedente){
        return new CedenteResponse(cedente.getId(), cedente.getCnpj(), cedente.getRazaoSocial());
    }
}
