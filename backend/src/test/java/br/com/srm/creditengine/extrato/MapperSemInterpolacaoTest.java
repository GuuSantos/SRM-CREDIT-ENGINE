package br.com.srm.creditengine.extrato;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSemInterpolacaoTest {

    private static final Path PASTA_MAPPERS = Path.of("src/main/resources/mapper");

    @Test
    void mappersUsamApenasParametrosVinculados() throws IOException {
        List<Path> mappers;
        try (Stream<Path> arquivos = Files.walk(PASTA_MAPPERS)) {
            mappers = arquivos.filter(p -> p.toString().endsWith(".xml")).toList();
        }

        assertThat(mappers).as("nenhum mapper encontrado: o teste estaria passando à toa").isNotEmpty();
        assertThat(mappers)
                .as("mappers com ${} (interpolação de texto no SQL)")
                .filteredOn(p -> ler(p).contains("${"))
                .isEmpty();
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}