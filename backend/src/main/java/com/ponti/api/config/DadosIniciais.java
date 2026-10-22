package com.ponti.api.config;

import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Cadastra alguns mentores e startups de exemplo quando o banco está vazio.
// Como o H2 em memória é apagado a cada reinício, isso mantém a demonstração sempre com dados.
// Se o banco já tiver registros (ex.: PostgreSQL), não faz nada.
@Component
public class DadosIniciais implements CommandLineRunner {

    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;

    public DadosIniciais(MentorRepository mentorRepository, StartupRepository startupRepository) {
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
    }

    @Override
    public void run(String... args) {
        if (mentorRepository.count() == 0) {
            mentorRepository.save(mentor(
                    "Juliana G.",
                    "Zootecnista e professora, com experiência ajudando startups de Health-techs a validar o produto com usuários reais.",
                    "Zootecnista, Professora",
                    "Health-techs, PetTech",
                    8));
            mentorRepository.save(mentor(
                    "Roberto",
                    "Veterinário com foco em PetTechs, ajuda startups a validar hipóteses de produto na área de saúde animal.",
                    "Veterinário",
                    "Health-techs, PetTech",
                    5));
        }

        if (startupRepository.count() == 0) {
            startupRepository.save(startup(
                    "PetCare Digital",
                    "Aplicativo que conecta tutores a veterinários para consultas rápidas e acompanhamento da saúde do pet.",
                    "PetTech", "Recife", "PE", 2));
            startupRepository.save(startup(
                    "SaúdeAgora",
                    "Plataforma de triagem online que organiza o atendimento inicial em clínicas pequenas.",
                    "Health-tech", "Olinda", "PE", 1));
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
}
