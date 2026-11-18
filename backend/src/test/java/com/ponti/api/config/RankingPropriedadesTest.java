package com.ponti.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

// Parâmetros ponti.ranking.*: os valores do application.properties e a validação ao subir.
// O ApplicationContextRunner sobe um "mini Spring" só com esta configuração (rápido, sem banco).
class RankingPropriedadesTest {

    @Configuration
    @EnableConfigurationProperties(RankingPropriedades.class)
    static class SoOsParametros {
    }

    // Lê o application.properties de verdade (ConfigDataApplicationContextInitializer)
    final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(SoOsParametros.class);

    @Test
    void applicationPropertiesTemOsValoresPadraoDoDocumento() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(RankingPropriedades.class)).isEqualTo(RankingPropriedades.padrao());
        });
    }

    @Test
    void permiteAjustarOsPesosDesdeQueSomem1() {
        contexto.withPropertyValues("ponti.ranking.peso-match=0.40", "ponti.ranking.peso-ocupacao=0.20")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(RankingPropriedades.class).pesoMatch()).isEqualTo(0.40);
                });
    }

    @Test
    void pesosQueNaoSomam1ImpedemAAplicacaoDeSubir() {
        contexto.withPropertyValues("ponti.ranking.peso-match=0.60")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause()
                            .hasMessageContaining("precisam somar 1, mas somam 1.15");
                });
    }

    @Test
    void pesoNegativoImpedeAAplicacaoDeSubir() {
        // Soma 1, mas com um peso negativo compensando outro
        contexto.withPropertyValues("ponti.ranking.peso-match=0.55", "ponti.ranking.peso-perfil=-0.05")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("não podem ser negativos");
                });
    }

    @Test
    void arredondamentoDeDoubleNaoReprovaPesosCorretos() {
        // 0.1 + 0.2 não é exatamente 0.3 em double; a tolerância evita falso erro
        RankingPropriedades p = new RankingPropriedades(0.1, 0.2, 0.3, 0.2, 0.1, 0.1, 0.1, 30, 3, 14, 30, 30, 5, 0.02);
        assertThat(p.pesoMatch()).isEqualTo(0.1);
    }

    @Test
    void limitesSemSentidoSaoRecusadosComMensagemEmPortugues() {
        assertThatThrownBy(() -> new RankingPropriedades(0.45, 0.15, 0.15, 0.15, 0.05, 0.05,
                0.10, 0, 3, 14, 30, 30, 5, 0.02))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dias-novo precisa ser maior que zero");
    }
}
