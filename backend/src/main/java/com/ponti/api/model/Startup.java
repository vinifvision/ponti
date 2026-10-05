package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @Column(length = 2000)
    private String descricao;

    private String videoPitchUrl;

    @Column(length = 2000)
    private String atuacoes;

    private String segmento;
    private String cidade;
    private String estado;
    private String site;

    // Escala de maturidade (inspirada no TRL): 1 a 9
    @Min(1) @Max(9)
    private Integer estagioMaturidade;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getVideoPitchUrl() { return videoPitchUrl; }
    public void setVideoPitchUrl(String videoPitchUrl) { this.videoPitchUrl = videoPitchUrl; }
    public String getAtuacoes() { return atuacoes; }
    public void setAtuacoes(String atuacoes) { this.atuacoes = atuacoes; }
    public String getSegmento() { return segmento; }
    public void setSegmento(String segmento) { this.segmento = segmento; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public Integer getEstagioMaturidade() { return estagioMaturidade; }
    public void setEstagioMaturidade(Integer estagioMaturidade) { this.estagioMaturidade = estagioMaturidade; }
}
