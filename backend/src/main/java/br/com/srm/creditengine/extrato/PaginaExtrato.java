package br.com.srm.creditengine.extrato;

import java.util.List;

public record PaginaExtrato(
        List<ItemExtrato> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas
) {
}