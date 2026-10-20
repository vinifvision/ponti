package com.ponti.api.repository;

import com.ponti.api.model.Convite;
import com.ponti.api.model.StatusConvite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

    // Impede o mesmo convite (startup + mentor) de ser criado duas vezes
    boolean existsByStartupIdAndMentorId(Long startupId, Long mentorId);

    // Inbox do mentor e lista de convites enviados pela startup
    List<Convite> findByMentorId(Long mentorId);

    List<Convite> findByStartupId(Long startupId);

    List<Convite> findByMentorIdAndStatus(Long mentorId, StatusConvite status);

    List<Convite> findByStartupIdAndStatus(Long startupId, StatusConvite status);

    List<Convite> findByStatus(StatusConvite status);
}
