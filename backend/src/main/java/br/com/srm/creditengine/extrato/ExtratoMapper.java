package br.com.srm.creditengine.extrato;

import br.com.srm.creditengine.precificacao.Moeda;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface ExtratoMapper {

    long contar(@Param("inicio") OffsetDateTime inicio,
                @Param("fim") OffsetDateTime fim,
                @Param("cnpj") String cnpj,
                @Param("moeda") Moeda moeda);

    List<ItemExtrato> buscar(@Param("inicio") OffsetDateTime inicio,
                             @Param("fim") OffsetDateTime fim,
                             @Param("cnpj") String cnpj,
                             @Param("moeda") Moeda moeda,
                             @Param("ordenarPor") String ordenarPor,
                             @Param("crescente") boolean crescente,
                             @Param("limite") int limite,
                             @Param("deslocamento") long deslocamento);
}