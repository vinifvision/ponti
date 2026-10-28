package com.ponti.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ponti.api.dto.MentorResposta;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.MentorService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Com os dados de demonstração ligados, a plataforma sobe "em uso": vários mentores com reputação,
// startups e mentorias em todos os estados.
// Banco separado, para os dados de demonstração não aparecerem nos outros testes
@SpringBootTest(properties = {"ponti.demo.completo=true", "spring.datasource.url=jdbc:h2:mem:ponti-demo"})
class DadosDemonstracaoTest {

    @Autowired MentorService mentorService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AgendamentoRepository agendamentoRepository;

    @Test
    void carregaMentoresComReputacaoEHistorico() {
        List<MentorResposta> mentores = mentorService.listar();

        assertThat(mentores).hasSizeGreaterThanOrEqualTo(12);
        assertThat(mentores).allSatisfy(m -> assertThat(m.fotoUrl()).startsWith("/demo/mentores/"));
        MentorResposta juliana = mentores.stream().filter(m -> m.nome().equals("Juliana G.")).findFirst().orElseThrow();
        assertThat(juliana.notaMedia()).isBetween(4.0, 5.0);
        assertThat(juliana.totalMentorias()).isGreaterThanOrEqualTo(4);
        assertThat(usuarioRepository.findByEmail("finfacil@exemplo.com")).isPresent();
        Long petcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow().getStartupId();
        assertThat(agendamentoRepository.findByStartupIdOrderByInicioAsc(petcare)).hasSizeGreaterThanOrEqualTo(5);
    }
}
