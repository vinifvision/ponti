package com.ponti.api.repository;

import com.ponti.api.model.Atividade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    // Linha do tempo: do prazo mais próximo para o mais distante
    List<Atividade> findByAcompanhamentoIdOrderByPrazoAscIdAsc(Long acompanhamentoId);

    void deleteByAcompanhamentoId(Long acompanhamentoId);
}
