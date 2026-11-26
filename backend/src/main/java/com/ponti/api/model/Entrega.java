package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// O que a startup enviou numa atividade. Cada reenvio é uma entrega nova (fica o histórico).
@Entity
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long atividadeId;

    @Column(length = 4000)
    private String texto;

    @Column(length = 500)
    private String link;

    @NotNull
    private LocalDateTime enviadaEm;

    // Resposta do mentor ao aprovar (opcional) ou devolver (obrigatória)
    @Column(length = 2000)
    private String feedback;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAtividadeId() { return atividadeId; }
    public void setAtividadeId(Long atividadeId) { this.atividadeId = atividadeId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
    public void setEnviadaEm(LocalDateTime enviadaEm) { this.enviadaEm = enviadaEm; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
