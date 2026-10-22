package com.ponti.api.config;

import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cadastra alguns mentores e startups de exemplo quando o banco está vazio.
// Como o H2 em memória é apagado a cada reinício, isso mantém a demonstração sempre com dados.
// Se o banco já tiver registros (ex.: PostgreSQL), não faz nada.
//
// Cada perfil de exemplo ganha um usuário para testar o login, todos com a senha SENHA_EXEMPLO:
//   juliana@exemplo.com, roberto@exemplo.com (mentores)
//   petcare@exemplo.com, saudeagora@exemplo.com (startups)
@Component
public class DadosIniciais implements CommandLineRunner {

    public static final String SENHA_EXEMPLO = "ponti1234";

    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DadosIniciais(MentorRepository mentorRepository,
                         StartupRepository startupRepository,
                         UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder) {
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (mentorRepository.count() == 0) {
            Mentor juliana = mentorRepository.save(mentor(
                    "Juliana G.",
                    "Zootecnista e professora, com experiência ajudando startups de Health-techs a validar o produto com usuários reais.",
                    "Zootecnista, Professora",
                    "Health-techs, PetTech",
                    8));
            Mentor roberto = mentorRepository.save(mentor(
                    "Roberto",
                    "Veterinário com foco em PetTechs, ajuda startups a validar hipóteses de produto na área de saúde animal.",
                    "Veterinário",
                    "Health-techs, PetTech",
                    5));
            usuarioRepository.save(usuarioDoMentor("juliana@exemplo.com", juliana));
            usuarioRepository.save(usuarioDoMentor("roberto@exemplo.com", roberto));
        }

        if (startupRepository.count() == 0) {
            Startup petCare = startupRepository.save(startup(
                    "PetCare Digital",
                    "Aplicativo que conecta tutores a veterinários para consultas rápidas e acompanhamento da saúde do pet.",
                    "PetTech", "Recife", "PE", 2));
            Startup saudeAgora = startupRepository.save(startup(
                    "SaúdeAgora",
                    "Plataforma de triagem online que organiza o atendimento inicial em clínicas pequenas.",
                    "Health-tech", "Olinda", "PE", 1));
            usuarioRepository.save(usuarioDaStartup("petcare@exemplo.com", petCare));
            usuarioRepository.save(usuarioDaStartup("saudeagora@exemplo.com", saudeAgora));
        }
    }

    private Mentor mentor(String nome, String bio, String areasAtuacao, String ecossistemas,
                          Integer anosExperiencia) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setBio(bio);
        m.setAreasAtuacao(areasAtuacao);
        m.setEcossistemas(ecossistemas);
        m.setAnosExperiencia(anosExperiencia);
        m.setValorSessao(BigDecimal.ZERO);
        return m;
    }

    private Startup startup(String nome, String descricao, String segmento,
                            String cidade, String estado, Integer estagioMaturidade) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setSegmento(segmento);
        s.setCidade(cidade);
        s.setEstado(estado);
        s.setEstagioMaturidade(estagioMaturidade);
        return s;
    }

    private Usuario usuarioDoMentor(String email, Mentor mentor) {
        Usuario u = usuario(email, TipoUsuario.MENTOR);
        u.setMentorId(mentor.getId());
        return u;
    }

    private Usuario usuarioDaStartup(String email, Startup startup) {
        Usuario u = usuario(email, TipoUsuario.STARTUP);
        u.setStartupId(startup.getId());
        return u;
    }

    private Usuario usuario(String email, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(SENHA_EXEMPLO));
        u.setTipo(tipo);
        return u;
    }
}
