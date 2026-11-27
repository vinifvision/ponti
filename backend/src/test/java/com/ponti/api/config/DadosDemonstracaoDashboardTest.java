package com.ponti.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ponti.api.model.Avaliacao;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Dados SIMULADOS do dashboard do admin: carregam as jornadas extras, preenchem a data de cadastro
// das contas antigas e não duplicam nada ao reiniciar. Banco separado, para não afetar os outros testes.
@SpringBootTest(properties = {"ponti.demo.completo=true", "spring.datasource.url=jdbc:h2:mem:ponti-demo-dashboard"})
class DadosDemonstracaoDashboardTest {

    @Autowired DadosDemonstracaoDashboard dados;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired StartupRepository startupRepository;
    @Autowired MentorRepository mentorRepository;
    @Autowired ConviteRepository conviteRepository;
    @Autowired AvaliacaoRepository avaliacaoRepository;

    @Test
    void carregaAsStartupsEOsMentoresExtras() {
        assertThat(startupRepository.count()).isEqualTo(6 + 13);
        assertThat(mentorRepository.count()).isEqualTo(12 + 2);
        assertThat(usuarioRepository.findByEmail("henrique@exemplo.com")).isPresent();
        assertThat(usuarioRepository.findByEmail("cuidaidoso@exemplo.com")).isPresent();
    }

    @Test
    void todaContaTemDataDeCadastroEspalhadaNoTempo() {
        assertThat(usuarioRepository.findAll()).allMatch(u -> u.getCriadoEm() != null);

        Instant ha30Dias = Instant.now().minus(Duration.ofDays(30));
        long startupsNoUltimoMes = usuarioRepository.findAll().stream()
                .filter(u -> u.getTipo() == TipoUsuario.STARTUP)
                .filter(u -> u.getCriadoEm().isAfter(ha30Dias)).count();
        assertThat(startupsNoUltimoMes).isEqualTo(9);

        // As contas base foram criadas "agora" ao subir; a data é estimada pela primeira atividade
        Usuario petcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow();
        assertThat(petcare.getCriadoEm()).isBefore(ha30Dias);
    }

    @Test
    void todoConviteTemDataDeEnvio() {
        assertThat(conviteRepository.findAll()).isNotEmpty().allMatch(c -> c.getCriadoEm() != null);
    }

    @Test
    void doisMentoresMalAvaliados() {
        for (String email : new String[]{"henrique@exemplo.com", "sergio@exemplo.com"}) {
            Long mentorId = usuarioRepository.findByEmail(email).orElseThrow().getMentorId();
            double media = avaliacaoRepository.findAll().stream()
                    .filter(a -> mentorId.equals(a.getMentorId()))
                    .mapToInt(Avaliacao::getNota).average().orElseThrow();
            assertThat(media).isLessThan(3.5);
        }
    }

    @Test
    void naoDuplicaAoReiniciar() {
        long startups = startupRepository.count();
        long convites = conviteRepository.count();
        Instant cadastroPetcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow().getCriadoEm();

        dados.run();

        assertThat(startupRepository.count()).isEqualTo(startups);
        assertThat(conviteRepository.count()).isEqualTo(convites);
        assertThat(usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow().getCriadoEm())
                .isEqualTo(cadastroPetcare);
    }
}
