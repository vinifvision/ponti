package com.ponti.api.repository;

import com.ponti.api.model.Certificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificacaoRepository extends JpaRepository<Certificacao, Long> {

    // Mais recentes primeiro, como no perfil
    List<Certificacao> findByMentorIdOrderByAnoInicioDescIdDesc(Long mentorId);

    List<Certificacao> findByStartupIdOrderByAnoInicioDescIdDesc(Long startupId);
}
