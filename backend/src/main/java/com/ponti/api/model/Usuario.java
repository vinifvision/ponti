package com.ponti.api.model;

import jakarta.persistence.*;

// Conta de acesso. Cada conta aponta para o perfil de uma startup OU de um mentor.
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome de login, sempre em minúsculas
    @Column(unique = true, nullable = false)
    private String usuario;

    // Senha criptografada com BCrypt (nunca a senha em texto)
    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipo;

    private Long startupId;
    private Long mentorId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public TipoUsuario getTipo() { return tipo; }
    public void setTipo(TipoUsuario tipo) { this.tipo = tipo; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
}
