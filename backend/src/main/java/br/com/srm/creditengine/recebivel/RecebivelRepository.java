package br.com.srm.creditengine.recebivel;

import br.com.srm.creditengine.precificacao.TipoRecebivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecebivelRepository extends JpaRepository<Recebivel, Long> {

    Optional<Recebivel> findByCedenteIdAndTipoAndNumero(Long cedenteId, TipoRecebivel tipo, String numero);

}
