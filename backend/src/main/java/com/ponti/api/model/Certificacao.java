package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Certificação ou experiência que aparece no perfil (ex.: "Mentoria programa Porto Digital, 2025 - 2026").
// Pertence a um mentor OU a uma startup (o campo do outro fica nulo).
@Entity
public class Certificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long mentorId;
    private Long startupId;

    @NotBlank @Size(max = 255)
    private String titulo;

    @Size(max = 255)
    private String instituicao;

    @Min(1950) @Max(2100)
    private Integer anoInicio;

    // Vazio = ainda em andamento
    @Min(1950) @Max(2100)
    private Integer anoFim;

    @Size(max = 1000)
    @Column(length = 1000)
    private String descricao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getInstituicao() { return instituicao; }
    public void setInstituicao(String instituicao) { this.instituicao = instituicao; }
    public Integer getAnoInicio() { return anoInicio; }
    public void setAnoInicio(Integer anoInicio) { this.anoInicio = anoInicio; }
    public Integer getAnoFim() { return anoFim; }
    public void setAnoFim(Integer anoFim) { this.anoFim = anoFim; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
