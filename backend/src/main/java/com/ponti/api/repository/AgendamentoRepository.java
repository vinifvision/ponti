package com.ponti.api.repository;

import com.ponti.api.model.Agendamento;
import com.ponti.api.model.StatusAgendamento;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByStartupIdOrderByInicioAsc(Long startupId);

    List<Agendamento> findByMentorIdOrderByInicioAsc(Long mentorId);

    // Sessões do mentor que ainda ocupam horário (pendentes ou confirmadas) dentro de um período
    @Query("""
            select a from Agendamento a
            where a.mentorId = :mentorId and a.status in :status
              and a.inicio < :ate and a.fim > :de
            """)
    List<Agendamento> ocupandoHorario(@Param("mentorId") Long mentorId,
                                      @Param("status") Collection<StatusAgendamento> status,
                                      @Param("de") LocalDateTime de,
                                      @Param("ate") LocalDateTime ate);
}
