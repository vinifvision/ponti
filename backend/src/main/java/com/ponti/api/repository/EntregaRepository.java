package com.ponti.api.repository;

import com.ponti.api.model.Entrega;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    // Histórico de envios, do primeiro ao último
    List<Entrega> findByAtividadeIdOrderByIdAsc(Long atividadeId);

    void deleteByAtividadeId(Long atividadeId);
}
