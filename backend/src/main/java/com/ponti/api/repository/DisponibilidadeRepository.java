package com.ponti.api.repository;

import com.ponti.api.model.Disponibilidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {

    List<Disponibilidade> findByMentorId(Long mentorId);

    void deleteByMentorId(Long mentorId);
}
