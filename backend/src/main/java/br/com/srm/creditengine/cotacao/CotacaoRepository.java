package br.com.srm.creditengine.cotacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface CotacaoRepository extends JpaRepository<Cotacao, UUID> {
}
