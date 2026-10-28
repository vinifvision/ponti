package com.ponti.api.repository;

import com.ponti.api.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Impede dois cadastros com o mesmo e-mail
    boolean existsByEmail(String email);

    // Usado pelo login
    Optional<Usuario> findByEmail(String email);

    // Usado ao excluir a conta (ver ContaService)
    void deleteByStartupId(Long startupId);
    void deleteByMentorId(Long mentorId);
}
