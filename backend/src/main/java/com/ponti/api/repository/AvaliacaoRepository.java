package com.ponti.api.repository;

import com.ponti.api.model.Avaliacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByAgendamentoId(Long agendamentoId);

    List<Avaliacao> findByAgendamentoIdIn(List<Long> agendamentoIds);

    List<Avaliacao> findByMentorIdOrderByCriadoEmDesc(Long mentorId);

    // Média e quantidade de avaliações de cada mentor: [mentorId, média, quantidade]
    @Query("select a.mentorId, avg(a.nota), count(a) from Avaliacao a group by a.mentorId")
    List<Object[]> resumoPorMentor();

    void deleteByMentorId(Long mentorId);
    void deleteByStartupId(Long startupId);
}
