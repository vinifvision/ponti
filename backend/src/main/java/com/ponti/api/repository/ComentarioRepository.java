package com.ponti.api.repository;

import com.ponti.api.model.Comentario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // Conversa de uma entrega, do mais antigo ao mais novo
    List<Comentario> findByEntregaIdOrderByIdAsc(Long entregaId);

    void deleteByEntregaId(Long entregaId);
}
