package com.ponti.api.repository;

import com.ponti.api.model.Avaliacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByAgendamentoId(Long agendamentoId);

    List<Avaliacao> findByAgendamentoIdIn(List<Long> agendamentoIds);

    List<Avaliacao> findByMentorIdOrderByCriadoEmDesc(Long mentorId);

    // Média e quantidade de avaliações de cada mentor: [mentorId, média, quantidade]
    @Query("select a.mentorId, avg(a.nota), count(a) from Avaliacao a group by a.mentorId")
    List<Object[]> resumoPorMentor();

    // Soma e quantidade das notas, para o ranking (a média bayesiana precisa da soma exata):
    // de cada mentor [mentorId, soma, quantidade], de um mentor [soma, quantidade] e da plataforma toda
    @Query("select a.mentorId, sum(a.nota), count(a) from Avaliacao a group by a.mentorId")
    List<Object[]> somaPorMentor();

    @Query("select coalesce(sum(a.nota), 0), count(a) from Avaliacao a where a.mentorId = :mentorId")
    List<Object[]> somaDoMentor(@Param("mentorId") Long mentorId);

    @Query("select coalesce(sum(a.nota), 0), count(a) from Avaliacao a")
    List<Object[]> somaGeral();

    void deleteByMentorId(Long mentorId);
    void deleteByStartupId(Long startupId);
}
