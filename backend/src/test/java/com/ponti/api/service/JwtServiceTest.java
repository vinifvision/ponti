package com.ponti.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// TESTE DE UNIDADE do JwtService: criamos o objeto com "new", sem Spring.
// Assim dá para escolher o segredo e a validade de cada teste (ex.: um token que já nasce vencido).
class JwtServiceTest {

    static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    JwtService jwt = new JwtService(SEGREDO, Duration.ofHours(1));

    static Usuario usuario(long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("ana@email.com");
        u.setTipo(TipoUsuario.STARTUP);
        return u;
    }

    // Verifica que a chamada falha com 401 (é o que o front recebe quando o token não presta)
    static void assertRejeitado(Runnable chamada) {
        assertThatThrownBy(chamada::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenGeradoEValidadoDevolveOIdDoUsuario() {
        JwtService.Token token = jwt.gerar(usuario(42));

        assertThat(jwt.validar(token.valor())).isEqualTo(42L);
    }

    @Test
    void tokenExpiraNoPrazoConfigurado() {
        Instant antes = Instant.now();
        JwtService.Token token = jwt.gerar(usuario(1));

        // Margem de 5 segundos para a lentidão da própria execução do teste
        assertThat(token.expiraEm()).isBetween(antes.plus(Duration.ofHours(1)).minusSeconds(5),
                                               antes.plus(Duration.ofHours(1)).plusSeconds(5));
    }

    @Test
    void rejeitaTokenVencido() {
        // Validade negativa: o token já nasce vencido
        JwtService jwtVencido = new JwtService(SEGREDO, Duration.ofSeconds(-10));
        String token = jwtVencido.gerar(usuario(1)).valor();

        assertRejeitado(() -> jwt.validar(token));
    }

    @Test
    void rejeitaTokenAdulterado() {
        // Um JWT tem 3 partes: cabeçalho.dados.assinatura. Trocar os dados (ex.: o id do usuário)
        // sem conhecer o segredo faz a assinatura deixar de bater.
        String[] partes = jwt.gerar(usuario(1)).valor().split("\\.");
        String dadosDeOutroToken = jwt.gerar(usuario(999)).valor().split("\\.")[1];
        String adulterado = partes[0] + "." + dadosDeOutroToken + "." + partes[2];

        assertRejeitado(() -> jwt.validar(adulterado));
    }

    @Test
    void rejeitaTokenAssinadoComOutroSegredo() {
        JwtService outroServidor = new JwtService("um-segredo-completamente-diferente-1234567", Duration.ofHours(1));
        String token = outroServidor.gerar(usuario(1)).valor();

        assertRejeitado(() -> jwt.validar(token));
    }

    @Test
    void rejeitaTextoQueNaoEToken() {
        assertRejeitado(() -> jwt.validar("isso-nao-e-um-token"));
        assertRejeitado(() -> jwt.validar(""));
    }

    @Test
    void naoAceitaSegredoCurto() {
        // Segredo curto é fácil de adivinhar por força bruta, então a aplicação nem sobe
        assertThatThrownBy(() -> new JwtService("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
