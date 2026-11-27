package com.ponti.api.service;

import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

// Perfis cujas contas o admin bloqueou. Eles somem das listas, das buscas e das recomendações.
@Service
public class ContasBloqueadas {

    private final UsuarioRepository usuarioRepository;

    public ContasBloqueadas(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Set<Long> mentores() {
        return usuarioRepository.findByBloqueadoTrue().stream()
                .map(Usuario::getMentorId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public Set<Long> startups() {
        return usuarioRepository.findByBloqueadoTrue().stream()
                .map(Usuario::getStartupId).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
