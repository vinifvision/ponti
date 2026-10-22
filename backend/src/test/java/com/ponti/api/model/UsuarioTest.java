package com.ponti.api.model;

import static org.assertj.core.api.Assertions.assertThat;

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
}
