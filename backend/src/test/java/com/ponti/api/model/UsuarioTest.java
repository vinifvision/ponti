package com.ponti.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

// TESTE DE UNIDADE: testa um método isolado, sem subir o Spring nem banco de dados.
// É o tipo de teste mais rápido (milissegundos) e o mais fácil de entender.
class UsuarioTest {

    @Test
    void normalizarEmailRemoveEspacosEPassaParaMinusculas() {
        assertThat(Usuario.normalizarEmail("  Ana.Souza@Email.COM ")).isEqualTo("ana.souza@email.com");
    }

    @Test
    void normalizarEmailNaoMexeEmEmailJaNormalizado() {
        assertThat(Usuario.normalizarEmail("ana@email.com")).isEqualTo("ana@email.com");
    }

    @Test
    void normalizarEmailAceitaNulo() {
        // Sem isso, um JSON sem "email" daria NullPointerException (erro 500) em vez de erro de validação (400)
        assertThat(Usuario.normalizarEmail(null)).isNull();
    }

    // ---- Último acesso (usado pelo ranking de mentores) ----

    static final Instant AGORA = Instant.parse("2026-11-18T12:00:00Z");

    @Test
    void primeiroAcessoSempreGrava() {
        Usuario u = new Usuario();
        assertThat(u.registrarAcesso(AGORA)).isTrue();
        assertThat(u.getUltimoAcessoEm()).isEqualTo(AGORA);
    }

    @Test
    void acessoMenosDeUmaHoraDepoisNaoGravaDeNovo() {
        Usuario u = new Usuario();
        u.setUltimoAcessoEm(AGORA);
        assertThat(u.registrarAcesso(AGORA.plus(Duration.ofMinutes(59)))).isFalse();
        assertThat(u.getUltimoAcessoEm()).isEqualTo(AGORA);
    }

    @Test
    void acessoUmaHoraDepoisGrava() {
        Usuario u = new Usuario();
        u.setUltimoAcessoEm(AGORA);
        assertThat(u.registrarAcesso(AGORA.plus(Duration.ofHours(1)))).isTrue();
        assertThat(u.getUltimoAcessoEm()).isEqualTo(AGORA.plus(Duration.ofHours(1)));
    }
}
