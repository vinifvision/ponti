package com.ponti.api.repository;

import com.ponti.api.model.Meta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaRepository extends JpaRepository<Meta, Long> {

    List<Meta> findByAcompanhamentoIdOrderByOrdemAsc(Long acompanhamentoId);
}
