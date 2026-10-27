package br.com.srm.creditengine.taxas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface TaxaBaseRepository extends JpaRepository<TaxaBase, Long> {
    Optional<TaxaBase> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(Instant instante);
}
