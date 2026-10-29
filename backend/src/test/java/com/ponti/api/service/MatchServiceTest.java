package com.ponti.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ponti.api.model.Tema;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Testes de unidade do cálculo do match: sem banco, só as contas.
// Regra: 75 pontos pelos temas cobertos + 25 pelo setor (metade se o mentor atende "todas as áreas").
class MatchServiceTest {

    static Set<Tema> temas(Tema... t) {
        return new LinkedHashSet<>(java.util.List.of(t));
    }

    @Test
    void cobreTodosOsTemasEOSetor_da100() {
        var r = MatchService.calcular(temas(Tema.VALIDACAO, Tema.PITCH), "PetTech",
                temas(Tema.PITCH, Tema.VALIDACAO, Tema.VENDAS), "Health-techs, PetTech", true);
        assertThat(r.pontos()).isEqualTo(100);
        assertThat(r.motivos()).contains("Conhece o setor PetTech");
        assertThat(r.motivos().get(0)).startsWith("Ajuda com Validação de ideia e Pitch e captação (2 de 2");
    }

    @Test
    void cobreDoisDeTresTemasESetor_da75() {
        var r = MatchService.calcular(temas(Tema.VALIDACAO, Tema.PITCH, Tema.TECNOLOGIA), "PetTech",
                temas(Tema.VALIDACAO, Tema.PITCH), "PetTech", true);
        assertThat(r.pontos()).isEqualTo(75); // 75 * 2/3 = 50, mais 25 do setor
        assertThat(r.motivos().get(0)).contains("2 de 3");
    }

    @Test
    void semTemaEmComumMasSetorCerto_da25() {
        var r = MatchService.calcular(temas(Tema.JURIDICO), "Fintech", temas(Tema.VENDAS), "Fintechs", true);
        assertThat(r.pontos()).isEqualTo(25);
        assertThat(r.motivos()).contains("Não atua nos temas que você escolheu");
    }

    @Test
    void mentorQueAtendeTodasAsAreasGanhaMetadeDoSetor() {
        var r = MatchService.calcular(temas(Tema.JURIDICO), "Fintech", temas(Tema.VENDAS), "Todas as áreas", true);
        assertThat(r.pontos()).isEqualTo(13); // 12,5 arredondado
        assertThat(r.motivos()).contains("Atende todos os setores");
    }

    @Test
    void semNecessidadesNaoHaCompatibilidadeEAvisaOQueFazer() {
        var r = MatchService.calcular(Set.of(), "PetTech", temas(Tema.PITCH), "PetTech", true);
        assertThat(r.pontos()).isEqualTo(25); // só o setor conta
        assertThat(r.motivos().get(0)).contains("Escolha os temas");
    }

    @Test
    void setorIgnoraAcentoMaiusculaEPlural() {
        assertThat(MatchService.afinidadeDeSetor("Health-tech", "SaaS, Health-techs")).isEqualTo(1.0);
        assertThat(MatchService.afinidadeDeSetor("Saúde", "saude")).isEqualTo(1.0);
        assertThat(MatchService.afinidadeDeSetor("Agritech", "Fintechs, SaaS")).isEqualTo(0.0);
        assertThat(MatchService.afinidadeDeSetor(null, "Fintechs")).isEqualTo(0.0);
        assertThat(MatchService.afinidadeDeSetor("Fintech", null)).isEqualTo(0.0);
    }

    @Test
    void textoParaOMentorFalaComOMentor() {
        var r = MatchService.calcular(temas(Tema.PITCH), "Fintech", temas(Tema.PITCH), "Fintechs", false);
        assertThat(r.pontos()).isEqualTo(100);
        assertThat(r.motivos().get(0)).startsWith("Precisa de ajuda com Pitch e captação");
        assertThat(r.motivos()).contains("Você atua no setor Fintech");
    }
}
