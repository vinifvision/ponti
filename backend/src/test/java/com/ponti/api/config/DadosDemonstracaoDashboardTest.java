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
import com.ponti.api.dto.DashboardAdmin;
import com.ponti.api.service.DashboardService;
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
    @Autowired DashboardService dashboardService;

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

    // A história que o dashboard conta no pitch (últimos 30 dias). Se alguém mexer nos dados de
    // demonstração e quebrar essa história, este teste avisa.
    @Test
    void dashboardContaAHistoriaDoPitch() {
        DashboardAdmin d = dashboardService.gerar(null, null);

        // Funil: 9 cadastros e a maior perda logo no primeiro passo (startups que não pedem nada)
        assertThat(d.funil().etapas()).extracting(DashboardAdmin.EtapaFunil::quantidade).containsExactly(9L, 6L, 4L, 3L, 2L);
        assertThat(d.funil().maiorQueda()).isEqualTo("INTERESSE");

        // Prova do match: match alto vira conexão muito mais que match baixo
        assertThat(d.match().faixas().get(0).taxa()).isEqualTo(100);
        assertThat(d.match().faixas().get(2).taxa()).isLessThan(40);
        assertThat(d.match().diferenca()).isGreaterThanOrEqualTo(60);

        // Oferta x demanda: falta mentor de Marketing, Tecnologia e Jurídico
        assertThat(d.ofertaDemanda().subList(0, 3)).extracting(t -> t.tema().name())
                .containsExactly("MARKETING", "TECNOLOGIA", "JURIDICO");
        assertThat(d.ofertaDemanda().subList(0, 3)).allMatch(t -> t.situacao().equals("FALTA"));

        // Saúde: a Rota Verde parou
        assertThat(d.saude().parados()).extracting(DashboardAdmin.AcompanhamentoParado::startup).contains("Rota Verde");
        assertThat(d.saude().entregasAtrasadas()).isPositive();

        // Atenção: os dois mentores mal avaliados e startups sem conexão
        assertThat(d.atencao()).extracting(DashboardAdmin.Atencao::nome)
                .contains("Henrique Bastos", "Sérgio Leal", "Bairro Seguro", "VetNorte");
    }
}
