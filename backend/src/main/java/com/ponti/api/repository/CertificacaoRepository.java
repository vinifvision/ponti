package com.ponti.api.repository;

import com.ponti.api.model.Certificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CertificacaoRepository extends JpaRepository<Certificacao, Long> {

    // Mais recentes primeiro, como no perfil
    List<Certificacao> findByMentorIdOrderByAnoInicioDescIdDesc(Long mentorId);

    List<Certificacao> findByStartupIdOrderByAnoInicioDescIdDesc(Long startupId);

    // Mentores com ao menos uma certificação (item do "perfil completo" no ranking)
    @Query("select distinct c.mentorId from Certificacao c where c.mentorId is not null")
    List<Long> mentoresComCertificacao();

    // Usado ao excluir a conta (ver ContaService)
    void deleteByStartupId(Long startupId);
    void deleteByMentorId(Long mentorId);
}
