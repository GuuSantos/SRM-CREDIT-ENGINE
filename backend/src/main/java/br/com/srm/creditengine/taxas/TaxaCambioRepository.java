package br.com.srm.creditengine.taxas;

import br.com.srm.creditengine.precificacao.Moeda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface TaxaCambioRepository extends JpaRepository<TaxaCambio, Long> {
    Optional<TaxaCambio> findFirstByMoedaAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(Moeda moeda, Instant instante);

}
