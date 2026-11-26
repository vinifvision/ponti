package com.ponti.api.repository;

import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.StatusAcompanhamento;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcompanhamentoRepository extends JpaRepository<Acompanhamento, Long> {

    List<Acompanhamento> findByStartupIdAndStatusInOrderByCriadoEmDesc(Long startupId, Collection<StatusAcompanhamento> status);

    List<Acompanhamento> findByMentorIdAndStatusInOrderByCriadoEmDesc(Long mentorId, Collection<StatusAcompanhamento> status);

    // Todos os acompanhamentos de uma startup ou de um mentor (usado ao excluir a conta)
    List<Acompanhamento> findByStartupId(Long startupId);

    List<Acompanhamento> findByMentorId(Long mentorId);

    // Já existe proposta pendente ou acompanhamento ativo entre os dois?
    boolean existsByMentorIdAndStartupIdAndStatusIn(Long mentorId, Long startupId, Collection<StatusAcompanhamento> status);

    // Mentoria realizada = confirmada e com o horário já terminado (mesma regra de AgendamentoDetalhe)
    @Query("""
            select count(a) > 0 from Agendamento a
            where a.mentorId = :mentorId and a.startupId = :startupId
              and a.status = com.ponti.api.model.StatusAgendamento.CONFIRMADO and a.fim < :agora
            """)
    boolean teveMentoriaRealizada(@Param("mentorId") Long mentorId,
                                  @Param("startupId") Long startupId,
                                  @Param("agora") LocalDateTime agora);
}
