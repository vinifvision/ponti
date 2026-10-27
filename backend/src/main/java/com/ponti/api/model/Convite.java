package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long startupId;

    @NotNull
    private Long mentorId;

    @Enumerated(EnumType.STRING)
    private StatusConvite status = StatusConvite.PENDENTE;

    // Quem tomou a iniciativa: a startup convidou o mentor, ou o mentor convidou a startup.
    // Quem responde (aceita ou recusa) é sempre o outro lado. Se não vier, vale STARTUP.
    @Enumerated(EnumType.STRING)
    private TipoUsuario enviadoPor = TipoUsuario.STARTUP;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public StatusConvite getStatus() { return status; }
    public void setStatus(StatusConvite status) { this.status = status; }
    public TipoUsuario getEnviadoPor() { return enviadoPor; }
    public void setEnviadoPor(TipoUsuario enviadoPor) { this.enviadoPor = enviadoPor; }
}
