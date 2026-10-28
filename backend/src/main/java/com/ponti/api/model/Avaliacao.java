package com.ponti.api.model;

import jakarta.persistence.*;
import java.time.Instant;

// Nota (1 a 5) e comentário que a startup dá ao mentor depois de uma mentoria realizada.
// Uma avaliação por mentoria.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_agendamento", columnNames = "agendamentoId"))
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long agendamentoId;

    @Column(nullable = false)
    private Long mentorId;

    @Column(nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private Integer nota;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgendamentoId() { return agendamentoId; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
