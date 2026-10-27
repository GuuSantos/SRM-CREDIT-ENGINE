package br.com.srm.creditengine.liquidacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiquidacaoRepository extends JpaRepository<Liquidacao, Long> {
    Optional<Liquidacao> findByCotacaoId(UUID cotacaoId);
}
