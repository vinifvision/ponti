package com.ponti.api.service;

import com.ponti.api.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Gera e valida os tokens JWT do login.
// O token é assinado com o segredo (HMAC-SHA; a biblioteca escolhe HS256, HS384 ou HS512 conforme
// o tamanho do segredo). Se alguém alterar qualquer parte do token,
// a assinatura deixa de bater e ele é rejeitado. Por isso o segredo nunca pode vazar.
@Service
public class JwtService {

    public record Token(String valor, Instant expiraEm) {
    }

    private final SecretKey chave;
    private final Duration expiracao;

    public JwtService(@Value("${ponti.jwt.segredo}") String segredo,
                      @Value("${ponti.jwt.expiracao}") Duration expiracao) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ponti.jwt.segredo precisa ter pelo menos 32 caracteres");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracao = expiracao;
    }

    // O "subject" do token é o id do usuário. Tipo e e-mail vão junto só para facilitar a leitura no front.
    public Token gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracao);
        String valor = Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("tipo", usuario.getTipo().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new Token(valor, expiraEm);
    }

    // Confere assinatura e validade e devolve o id do usuário. Qualquer problema vira 401.
    public Long validar(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        }
    }
}
