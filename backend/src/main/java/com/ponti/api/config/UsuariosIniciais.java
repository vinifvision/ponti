package com.ponti.api.config;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cria contas de demonstração para as startups e mentores de exemplo.
// Roda quando a aplicação já está no ar (depois do DadosIniciais) e só se não houver nenhuma conta.
// Todas usam a senha "123456". São contas de exemplo: não use em produção.
@Component
public class UsuariosIniciais {

    private static final String SENHA_DEMO = "123456";

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuariosIniciais(UsuarioRepository usuarioRepository,
                            StartupRepository startupRepository,
                            MentorRepository mentorRepository,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void criarContasDeExemplo() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        contaDaStartup("petcare", "PetCare Digital");
        contaDaStartup("saudeagora", "SaúdeAgora");
        contaDoMentor("juliana", "Juliana G.");
        contaDoMentor("roberto", "Roberto");
    }

    private void contaDaStartup(String login, String nomeDaStartup) {
        startupRepository.findAll().stream()
                .filter(s -> nomeDaStartup.equals(s.getNome()))
                .findFirst()
                .ifPresent(s -> {
                    Usuario u = novo(login, TipoUsuario.STARTUP);
                    u.setStartupId(s.getId());
                    usuarioRepository.save(u);
                });
    }

    private void contaDoMentor(String login, String nomeDoMentor) {
        mentorRepository.findAll().stream()
                .filter(m -> nomeDoMentor.equals(m.getNome()))
                .findFirst()
                .ifPresent(m -> {
                    Usuario u = novo(login, TipoUsuario.MENTOR);
                    u.setMentorId(m.getId());
                    usuarioRepository.save(u);
                });
    }

    private Usuario novo(String login, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setUsuario(login);
        u.setSenhaHash(passwordEncoder.encode(SENHA_DEMO));
        u.setTipo(tipo);
        return u;
    }
}
