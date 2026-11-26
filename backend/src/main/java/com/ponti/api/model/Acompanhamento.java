package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

// Acompanhamento pós-mentoria: um mentor e uma startup trabalham num objetivo, com metas e atividades.
// Os números do card (aprovadas, atrasadas, pendências...) não ficam aqui: são contados a partir das
// atividades guardadas no banco, para nunca ficarem diferentes delas.
@Entity
public class Acompanhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long mentorId;

    @NotNull
    private Long startupId;

    @NotNull
    @Column(length = 500)
    private String objetivo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusAcompanhamento status = StatusAcompanhamento.PROPOSTO;

    // Preenchida quando a startup aceita
    private LocalDate dataInicio;

    // Preenchida quando o mentor encerra
    private LocalDate dataFim;

    private Instant criadoEm = Instant.now();

    public boolean ehParticipante(Long startupIdLogado, Long mentorIdLogado) {
        return (startupIdLogado != null && startupIdLogado.equals(startupId))
                || (mentorIdLogado != null && mentorIdLogado.equals(mentorId));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public StatusAcompanhamento getStatus() { return status; }
    public void setStatus(StatusAcompanhamento status) { this.status = status; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
