package com.ponti.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.Pagina;
import com.ponti.api.service.BuscaMentores.Filtro;
import com.ponti.api.service.BuscaMentores.Ordenacao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

// Testes de unidade da busca de mentores: as mesmas regras que a tela "Encontrar mentores" usava
// (experiencia.utils.ts e preco.utils.ts), agora no servidor. Sem banco.
class BuscaMentoresTest {

    static MentorResposta mentor(long id, String nome, Integer anos, String valor) {
        return mentor(id, nome, anos, valor, "Fintechs, SaaS", "Bio", "Produto");
    }

    static MentorResposta mentor(long id, String nome, Integer anos, String valor, String ecossistemas,
                                 String bio, String areas) {
        return new MentorResposta(id, nome, bio, areas, ecossistemas, Set.of(), anos,
                valor == null ? null : new BigDecimal(valor), 60, null, null, null, null, 0, 0);
    }

    static Filtro filtro(String q, String area, String experiencia, String faixaPreco, String ordenacao) {
        return Filtro.de(q, area, experiencia, faixaPreco, null, ordenacao, null, null, Ordenacao.RECOMENDADOS);
    }

    static boolean aceita(Filtro f, MentorResposta m) {
        return f.aceita(m, true);
    }

    // ---------------------------------------------------------------- faixas

    @Test
    void experienciaIgualAoFront() {
        Filtro iniciante = filtro(null, null, "iniciante", null, null);
        Filtro intermediario = filtro(null, null, "intermediario", null, null);
        Filtro senior = filtro(null, null, "senior", null, null);

        assertThat(aceita(iniciante, mentor(1, "A", 2, "10"))).isTrue();
        assertThat(aceita(iniciante, mentor(1, "A", 3, "10"))).isFalse();
        assertThat(aceita(intermediario, mentor(1, "A", 3, "10"))).isTrue();
        assertThat(aceita(intermediario, mentor(1, "A", 5, "10"))).isTrue();
        assertThat(aceita(senior, mentor(1, "A", 5, "10"))).isFalse();
        assertThat(aceita(senior, mentor(1, "A", 6, "10"))).isTrue();
        // Sem anosExperiencia não entra em nenhuma faixa (mas passa quando não há filtro)
        assertThat(aceita(iniciante, mentor(1, "A", null, "10"))).isFalse();
        assertThat(aceita(filtro(null, null, null, null, null), mentor(1, "A", null, "10"))).isTrue();
    }

    @Test
    void faixaDePrecoIgualAoFront() {
        Filtro gratuita = filtro(null, null, null, "gratuita", null);
        Filtro ate100 = filtro(null, null, null, "ate-100", null);
        Filtro de100a200 = filtro(null, null, null, "100-200", null);
        Filtro acima200 = filtro(null, null, null, "acima-200", null);

        assertThat(aceita(gratuita, mentor(1, "A", 5, "0"))).isTrue();
        assertThat(aceita(ate100, mentor(1, "A", 5, "0"))).isFalse(); // gratuito só aparece em "Gratuita"
        assertThat(aceita(ate100, mentor(1, "A", 5, "100.00"))).isTrue();
        assertThat(aceita(de100a200, mentor(1, "A", 5, "100.00"))).isFalse();
        assertThat(aceita(de100a200, mentor(1, "A", 5, "100.01"))).isTrue();
        assertThat(aceita(de100a200, mentor(1, "A", 5, "200"))).isTrue();
        assertThat(aceita(acima200, mentor(1, "A", 5, "200"))).isFalse();
        assertThat(aceita(acima200, mentor(1, "A", 5, "250"))).isTrue();
        // "A combinar" (sem valor) não entra em nenhuma faixa
        assertThat(aceita(gratuita, mentor(1, "A", 5, null))).isFalse();
    }

    // ---------------------------------------------------------------- texto e área

    @Test
    void buscaPorTextoOlhaNomeBioAreasEEcossistemasSemAcentoNemMaiuscula() {
        MentorResposta m = mentor(1, "Patrícia Nunes", 16, "220", "Health-techs, PetTech",
                "Regulatório em saúde", "Regulatório");

        assertThat(aceita(filtro("patricia", null, null, null, null), m)).isTrue();  // nome, sem acento
        assertThat(aceita(filtro("SAUDE", null, null, null, null), m)).isTrue();     // bio
        assertThat(aceita(filtro("regulat", null, null, null, null), m)).isTrue();   // áreas de atuação
        assertThat(aceita(filtro("pettech", null, null, null, null), m)).isTrue();   // ecossistemas
        assertThat(aceita(filtro("  juridico ", null, null, null, null), m)).isFalse();
    }

    @Test
    void areaPrecisaSerUmItemInteiroDosEcossistemas() {
        MentorResposta m = mentor(1, "A", 5, "10", "Health-techs, PetTech", "Bio", "X");

        assertThat(aceita(filtro(null, "PetTech", null, null, null), m)).isTrue();
        assertThat(aceita(filtro(null, "pettech", null, null, null), m)).isTrue();
        assertThat(aceita(filtro(null, "Pet", null, null, null), m)).isFalse(); // parte do nome não basta
    }

    @Test
    void comHorarioEm7DiasUsaOIndicadorDoRanking() {
        Filtro f = Filtro.de(null, null, null, null, true, null, null, null, Ordenacao.RECOMENDADOS);
        MentorResposta m = mentor(1, "A", 5, "10");

        assertThat(f.aceita(m, true)).isTrue();
        assertThat(f.aceita(m, false)).isFalse();
    }

    // ---------------------------------------------------------------- validação dos parâmetros

    @Test
    void valoresInvalidosDao400ComAsOpcoesAceitas() {
        assertThatThrownBy(() -> filtro(null, null, "expert", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Use um destes: iniciante, intermediario, senior");
        assertThatThrownBy(() -> filtro(null, null, null, "barato", null))
                .hasMessageContaining("faixaPreco");
        assertThatThrownBy(() -> filtro(null, null, null, null, "compatibilidade"))
                .hasMessageContaining("recomendados, nome, menor-valor, maior-valor");
        assertThatThrownBy(() -> Filtro.de(null, null, null, null, null, null, -1, null, Ordenacao.NOME))
                .hasMessageContaining("A página começa em 0");
        assertThatThrownBy(() -> Filtro.de(null, null, null, null, null, null, null, 51, Ordenacao.NOME))
                .hasMessageContaining("de 1 a 50");
        assertThatThrownBy(() -> Filtro.de(null, null, null, null, null, null, null, 0, Ordenacao.NOME))
                .hasMessageContaining("de 1 a 50");
    }

    @Test
    void parametrosVaziosValemComoSemFiltroEUsamOsPadroes() {
        Filtro f = Filtro.de(" ", "", "", "", null, "", null, null, Ordenacao.RECOMENDADOS);

        assertThat(f).isEqualTo(new Filtro(null, null, null, null, false, Ordenacao.RECOMENDADOS, 0, 12));
        assertThat(filtro(null, null, "SENIOR", null, "Menor-Valor").ordenacao()).isEqualTo(Ordenacao.MENOR_VALOR);
    }

    // ---------------------------------------------------------------- ordenação e paginação

    @Test
    void ordenacaoPorValorUsaAOrdemBaseComoDesempateEACombinarContaComoZero() {
        // Ordem base (a do ranking): 1, 2, 3, 4
        List<MentorResposta> base = List.of(
                mentor(1, "Zeca", 5, "100"),
                mentor(2, "Ana", 5, null),
                mentor(3, "Bia", 5, "100"),
                mentor(4, "Caio", 5, "0"));

        Pagina<MentorResposta> menor = BuscaMentores.buscar(base, filtro(null, null, null, null, "menor-valor"), m -> true, m -> m);
        Pagina<MentorResposta> maior = BuscaMentores.buscar(base, filtro(null, null, null, null, "maior-valor"), m -> true, m -> m);
        Pagina<MentorResposta> nome = BuscaMentores.buscar(base, filtro(null, null, null, null, "nome"), m -> true, m -> m);
        Pagina<MentorResposta> recomendados = BuscaMentores.buscar(base, filtro(null, null, null, null, null), m -> true, m -> m);

        assertThat(menor.itens()).extracting(MentorResposta::id).containsExactly(2L, 4L, 1L, 3L);
        assertThat(maior.itens()).extracting(MentorResposta::id).containsExactly(1L, 3L, 2L, 4L);
        assertThat(nome.itens()).extracting(MentorResposta::nome).containsExactly("Ana", "Bia", "Caio", "Zeca");
        assertThat(recomendados.itens()).extracting(MentorResposta::id).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void nomeComAcentoFicaNaOrdemDoPortugues() {
        List<MentorResposta> base = List.of(mentor(1, "Zé", 1, "1"), mentor(2, "Álvaro", 1, "1"), mentor(3, "Beto", 1, "1"));

        assertThat(BuscaMentores.buscar(base, filtro(null, null, null, null, "nome"), m -> true, m -> m).itens())
                .extracting(MentorResposta::nome).containsExactly("Álvaro", "Beto", "Zé");
    }

    @Test
    void paginacaoContaOsFiltradosENaoRepeteItens() {
        List<MentorResposta> base = IntStream.rangeClosed(1, 30)
                .mapToObj(i -> mentor(i, "M" + i, i % 2 == 0 ? 10 : 1, "50")).toList();
        Filtro senior = Filtro.de(null, null, "senior", null, null, null, 0, 6, Ordenacao.RECOMENDADOS);
        Filtro seniorPagina1 = Filtro.de(null, null, "senior", null, null, null, 1, 6, Ordenacao.RECOMENDADOS);

        Pagina<MentorResposta> p0 = BuscaMentores.buscar(base, senior, m -> senior.aceita(m, true), m -> m);
        Pagina<MentorResposta> p1 = BuscaMentores.buscar(base, seniorPagina1, m -> seniorPagina1.aceita(m, true), m -> m);

        assertThat(p0.totalItens()).isEqualTo(15); // só os 15 com 10 anos de experiência
        assertThat(p0.totalPaginas()).isEqualTo(3);
        assertThat(p0.itens()).hasSize(6);
        assertThat(p1.itens()).hasSize(6).doesNotContainAnyElementsOf(p0.itens());
    }

    @Test
    void paginaAlemDoFimVemVazia() {
        Pagina<Integer> p = Pagina.de(List.of(1, 2, 3), 5, 2);

        assertThat(p.itens()).isEmpty();
        assertThat(p.totalItens()).isEqualTo(3);
        assertThat(p.totalPaginas()).isEqualTo(2);
        assertThat(Pagina.de(List.<Integer>of(), 0, 12).totalPaginas()).isZero();
    }

    // ---------------------------------------------------------------- áreas

    @Test
    void areasJuntaOsEcossistemasDeTodosSemRepetirEmOrdemAlfabetica() {
        List<String> areas = BuscaMentores.areas(java.util.Arrays.asList(
                "Fintechs, SaaS", "SaaS,  Edtechs ", null, "", "Agritechs, Ética"));

        assertThat(areas).containsExactly("Agritechs", "Edtechs", "Ética", "Fintechs", "SaaS");
    }
}
