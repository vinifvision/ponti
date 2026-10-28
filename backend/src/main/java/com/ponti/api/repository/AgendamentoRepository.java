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

    // Pix que ainda esperam a "confirmação do banco" (ver GatewayPagamentoSimulado)
    List<Agendamento> findByStatusPagamento(com.ponti.api.model.StatusPagamento statusPagamento);

    // Mentorias já realizadas de cada mentor: [mentorId, quantidade]
    @Query("""
            select a.mentorId, count(a) from Agendamento a
            where a.status = com.ponti.api.model.StatusAgendamento.CONFIRMADO and a.fim < :agora
            group by a.mentorId
            """)
    List<Object[]> realizadasPorMentor(@Param("agora") LocalDateTime agora);

    // Usado ao excluir a conta (ver ContaService)
    void deleteByStartupId(Long startupId);
    void deleteByMentorId(Long mentorId);
}
