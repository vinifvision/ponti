package com.ponti.api.repository;

import com.ponti.api.model.MentorIndicadores;
import org.springframework.data.jpa.repository.JpaRepository;

// Indicadores pré-calculados do ranking (chave = id do mentor)
public interface MentorIndicadoresRepository extends JpaRepository<MentorIndicadores, Long> {
}
