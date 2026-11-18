package com.ponti.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

// Credenciais de acesso. Cada usuário é dono de um perfil: uma Startup OU um Mentor
// (o campo do outro tipo fica nulo).
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sempre gravado em minúsculas, para o login não depender de maiúsculas/minúsculas
    @NotBlank @Email
    @Column(nullable = false, unique = true)
    private String email;

    // Hash BCrypt da senha. Nunca a senha em texto puro, e nunca enviado nas respostas.
    @JsonIgnore
    @Column(nullable = false)
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipo;

    private Long startupId;
    private Long mentorId;

    // Último acesso à plataforma (login ou GET /auth/me). Nulo = desconhecido (registros antigos).
    @JsonIgnore
    private Instant ultimoAcessoEm;

    // Grava no máximo uma vez por hora, para não escrever no banco a cada requisição
    static final Duration INTERVALO_REGISTRO_ACESSO = Duration.ofHours(1);

    // Remove espaços nas pontas e passa para minúsculas: " Ana@Email.com " vira "ana@email.com"
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // Registra o acesso em "agora". Devolve true se mudou (aí quem chamou precisa salvar).
    public boolean registrarAcesso(Instant agora) {
        if (ultimoAcessoEm != null && agora.isBefore(ultimoAcessoEm.plus(INTERVALO_REGISTRO_ACESSO))) {
            return false;
        }
        ultimoAcessoEm = agora;
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public TipoUsuario getTipo() { return tipo; }
    public void setTipo(TipoUsuario tipo) { this.tipo = tipo; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Instant getUltimoAcessoEm() { return ultimoAcessoEm; }
    public void setUltimoAcessoEm(Instant ultimoAcessoEm) { this.ultimoAcessoEm = ultimoAcessoEm; }
}
