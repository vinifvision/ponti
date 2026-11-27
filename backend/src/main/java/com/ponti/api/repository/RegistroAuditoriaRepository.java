package com.ponti.api.repository;

import com.ponti.api.model.RegistroAuditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    // Mais recentes primeiro (tela "Registro" do admin)
    List<RegistroAuditoria> findAllByOrderByCriadoEmDescIdDesc();

    List<RegistroAuditoria> findByAlvoUsuarioIdOrderByCriadoEmDescIdDesc(Long alvoUsuarioId);
}
