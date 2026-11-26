package com.ponti.api.repository;

import com.ponti.api.model.Anexo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByAtividadeIdOrderByIdAsc(Long atividadeId);

    List<Anexo> findByEntregaIdOrderByIdAsc(Long entregaId);

    List<Anexo> findByAcompanhamentoId(Long acompanhamentoId);
}
