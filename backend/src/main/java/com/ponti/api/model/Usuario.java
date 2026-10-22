package com.ponti.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // Remove espaços nas pontas e passa para minúsculas: " Ana@Email.com " vira "ana@email.com"
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
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
}
