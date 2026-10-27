package com.ponti.api.config;

import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
    private final CertificacaoRepository certificacaoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final StartupRepository startupRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DadosIniciais(MentorRepository mentorRepository,
                         CertificacaoRepository certificacaoRepository,
                         DisponibilidadeRepository disponibilidadeRepository,
                         StartupRepository startupRepository,
                         UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder) {
        this.mentorRepository = mentorRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
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
                    8, new BigDecimal("120.00"), 60));
            juliana.setLinkedinUrl("https://www.linkedin.com/in/exemplo-juliana");
            juliana = mentorRepository.save(juliana);
            certificacaoRepository.save(certificacao(juliana, "Mentoria programa Porto Digital", "Porto Digital",
                    2026, 2026, "Mentoria de validação de produto para startups do programa de aceleração."));
            certificacaoRepository.save(certificacao(juliana, "Mentoria programa Sebrae", "Sebrae",
                    2025, 2026, "Acompanhamento de startups de saúde na fase de validação com usuários."));
            Mentor roberto = mentorRepository.save(mentor(
                    "Roberto",
                    "Veterinário com foco em PetTechs, ajuda startups a validar hipóteses de produto na área de saúde animal.",
                    "Veterinário",
                    "Health-techs, PetTech",
                    5, BigDecimal.ZERO, 45));
            // Horários em que cada mentor atende, para a agenda já aparecer com opções
            disponivel(juliana, DayOfWeek.TUESDAY, "09:00", "12:00");
            disponivel(juliana, DayOfWeek.THURSDAY, "09:00", "12:00");
            disponivel(juliana, DayOfWeek.THURSDAY, "14:00", "16:00");
            disponivel(roberto, DayOfWeek.MONDAY, "14:00", "17:00");
            disponivel(roberto, DayOfWeek.WEDNESDAY, "14:00", "17:00");
            disponivel(roberto, DayOfWeek.FRIDAY, "09:00", "11:15");
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
                          Integer anosExperiencia, BigDecimal valorSessao, Integer duracaoSessaoMinutos) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setBio(bio);
        m.setAreasAtuacao(areasAtuacao);
        m.setEcossistemas(ecossistemas);
        m.setAnosExperiencia(anosExperiencia);
        m.setValorSessao(valorSessao);
        m.setDuracaoSessaoMinutos(duracaoSessaoMinutos);
        return m;
    }

    private void disponivel(Mentor mentor, DayOfWeek dia, String inicio, String fim) {
        Disponibilidade d = new Disponibilidade();
        d.setMentorId(mentor.getId());
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFim(LocalTime.parse(fim));
        disponibilidadeRepository.save(d);
    }

    private Certificacao certificacao(Mentor mentor, String titulo, String instituicao,
                                      Integer anoInicio, Integer anoFim, String descricao) {
        Certificacao c = new Certificacao();
        c.setMentorId(mentor.getId());
        c.setTitulo(titulo);
        c.setInstituicao(instituicao);
        c.setAnoInicio(anoInicio);
        c.setAnoFim(anoFim);
        c.setDescricao(descricao);
        return c;
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
