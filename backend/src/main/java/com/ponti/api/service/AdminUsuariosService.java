package com.ponti.api.service;

import com.ponti.api.dto.PaginaAdmin;
import com.ponti.api.dto.UsuarioAdmin;
import com.ponti.api.model.AcaoAuditoria;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.RegistroAuditoria;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.RegistroAuditoriaRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.seguranca.Acesso;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Lista de usuários do admin, bloqueio e desbloqueio. Toda ação fica no registro de auditoria.
@Service
public class AdminUsuariosService {

    public enum Situacao { ATIVO, BLOQUEADO }

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final RegistroAuditoriaRepository auditoriaRepository;
    private final Acesso acesso;
    private final Clock relogio;

    public AdminUsuariosService(UsuarioRepository usuarioRepository, StartupRepository startupRepository,
                                MentorRepository mentorRepository, RegistroAuditoriaRepository auditoriaRepository,
                                Acesso acesso, Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.acesso = acesso;
        this.relogio = relogio;
    }

    // Busca por nome ou e-mail, filtro por tipo e por situação. Mais recentes primeiro.
    @Transactional(readOnly = true)
    public PaginaAdmin<UsuarioAdmin> listar(String busca, TipoUsuario tipo, Situacao situacao,
                                            Integer pagina, Integer tamanho) {
        Map<Long, String> startups = startupRepository.findAll().stream()
                .collect(Collectors.toMap(Startup::getId, Startup::getNome));
        Map<Long, String> mentores = mentorRepository.findAll().stream()
                .collect(Collectors.toMap(Mentor::getId, Mentor::getNome));
        Function<Usuario, String> nomeDe = u -> nome(u, startups, mentores);

        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        List<UsuarioAdmin> filtrados = usuarioRepository.findAll().stream()
                .filter(u -> tipo == null || u.getTipo() == tipo)
                .filter(u -> situacao == null || (situacao == Situacao.BLOQUEADO) == u.estaBloqueado())
                .map(u -> UsuarioAdmin.de(u, nomeDe.apply(u)))
                .filter(u -> termo.isEmpty()
                        || u.email().contains(termo)
                        || (u.nome() != null && u.nome().toLowerCase(Locale.ROOT).contains(termo)))
                .sorted(Comparator.comparing(UsuarioAdmin::criadoEm, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UsuarioAdmin::id, Comparator.reverseOrder()))
                .toList();
        return PaginaAdmin.de(filtrados, pagina, tamanho);
    }

    @Transactional
    public UsuarioAdmin bloquear(Long usuarioId, String motivo) {
        Usuario autor = autor();
        Usuario alvo = buscar(usuarioId);
        if (alvo.getTipo() == TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível bloquear uma conta de administrador");
        }
        if (alvo.estaBloqueado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta já está bloqueada");
        }
        Instant agora = Instant.now(relogio);
        alvo.bloquear(motivo.trim(), agora);
        usuarioRepository.save(alvo);
        String nome = nome(alvo);
        auditoriaRepository.save(new RegistroAuditoria(autor, AcaoAuditoria.BLOQUEAR_USUARIO, alvo.getId(),
                descricao(alvo, nome), motivo.trim(), agora));
        return UsuarioAdmin.de(alvo, nome);
    }

    @Transactional
    public UsuarioAdmin desbloquear(Long usuarioId, String motivo) {
        Usuario autor = autor();
        Usuario alvo = buscar(usuarioId);
        if (!alvo.estaBloqueado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta não está bloqueada");
        }
        alvo.desbloquear();
        usuarioRepository.save(alvo);
        String nome = nome(alvo);
        String texto = motivo == null || motivo.isBlank() ? null : motivo.trim();
        auditoriaRepository.save(new RegistroAuditoria(autor, AcaoAuditoria.DESBLOQUEAR_USUARIO, alvo.getId(),
                descricao(alvo, nome), texto, Instant.now(relogio)));
        return UsuarioAdmin.de(alvo, nome);
    }

    @Transactional(readOnly = true)
    public PaginaAdmin<RegistroAuditoria> auditoria(Integer pagina, Integer tamanho) {
        return PaginaAdmin.de(auditoriaRepository.findAllByOrderByCriadoEmDescIdDesc(), pagina, tamanho);
    }

    // ---------------------------------------------------------------- apoio

    private Usuario autor() {
        return usuarioRepository.findById(acesso.atual().usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar"));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private String nome(Usuario u) {
        if (u.getStartupId() != null) {
            return startupRepository.findById(u.getStartupId()).map(Startup::getNome).orElse(null);
        }
        if (u.getMentorId() != null) {
            return mentorRepository.findById(u.getMentorId()).map(Mentor::getNome).orElse(null);
        }
        return u.getTipo() == TipoUsuario.ADMIN ? "Administrador" : null;
    }

    private static String nome(Usuario u, Map<Long, String> startups, Map<Long, String> mentores) {
        if (u.getStartupId() != null) return startups.get(u.getStartupId());
        if (u.getMentorId() != null) return mentores.get(u.getMentorId());
        return u.getTipo() == TipoUsuario.ADMIN ? "Administrador" : null;
    }

    private static String descricao(Usuario u, String nome) {
        return (nome == null ? "Sem nome" : nome) + " (" + u.getEmail() + ")";
    }
}
