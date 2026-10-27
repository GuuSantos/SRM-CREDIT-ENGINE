package br.com.srm.creditengine.extrato;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ExtratoService {

    private static final ZoneId FUSO_OPERACAO = ZoneId.of("America/Sao_Paulo");

    private final ExtratoMapper mapper;


    public ExtratoService(ExtratoMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginaExtrato consultar(FiltroExtrato filtro) {
        OffsetDateTime inicio = inicioDoDia(filtro.dataInicio());
        OffsetDateTime fim = inicioDoDia(filtro.dataFim().plusDays(1));

        long total = mapper.contar(inicio, fim, filtro.cedenteCnpj(), filtro.moeda());
        List<ItemExtrato> itens = total == 0
                ? List.of()
                : mapper.buscar(inicio, fim, filtro.cedenteCnpj(), filtro.moeda(),
                filtro.ordenarPor().name(),
                filtro.direcao() == DirecaoOrdenacao.ASC,
                filtro.tamanho(),
                (long) filtro.pagina() * filtro.tamanho());

        int totalPaginas = (int) ((total + filtro.tamanho() - 1) / filtro.tamanho());
        return new PaginaExtrato(itens, filtro.pagina(), filtro.tamanho(), total, totalPaginas);
    }

    private static OffsetDateTime inicioDoDia(java.time.LocalDate data) {
        return data.atStartOfDay(FUSO_OPERACAO).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
