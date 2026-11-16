package com.ponti.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

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

    // Logo ou foto da startup
    @Size(max = 500)
    @Column(length = 500)
    private String fotoUrl;

    @Column(length = 2000)
    private String atuacoes;

    private String segmento;
    private String cidade;
    private String estado;
    private String site;

    // Contato privado e opcional: @JsonIgnore garante que nunca sai nas respostas da API
    // (só é mostrado ao mentor depois do agendamento, se a startup autorizar).
    @JsonIgnore
    @Column(length = 13)
    private String whatsapp;

    // Autorização do dono para mostrar o WhatsApp ao mentor nas mentorias CONFIRMADAS.
    // Começa desligada (null = não autorizou).
    @JsonIgnore
    private Boolean compartilharWhatsapp;

    // Temas em que a startup precisa de ajuda (até 3). É a base do match com os mentores.
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "startup_necessidades", joinColumns = @JoinColumn(name = "startup_id"))
    @Column(name = "tema")
    private Set<Tema> necessidades = new LinkedHashSet<>();

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
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
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
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public boolean isCompartilharWhatsapp() { return Boolean.TRUE.equals(compartilharWhatsapp); }
    public void setCompartilharWhatsapp(Boolean compartilharWhatsapp) { this.compartilharWhatsapp = compartilharWhatsapp; }

    // O número que pode ser mostrado à outra parte de uma mentoria confirmada (ou null)
    public String whatsappCompartilhado() { return isCompartilharWhatsapp() ? whatsapp : null; }
    public Set<Tema> getNecessidades() { return necessidades; }
    public void setNecessidades(Set<Tema> necessidades) { this.necessidades = necessidades == null ? new LinkedHashSet<>() : new LinkedHashSet<>(necessidades); }
    public Integer getEstagioMaturidade() { return estagioMaturidade; }
    public void setEstagioMaturidade(Integer estagioMaturidade) { this.estagioMaturidade = estagioMaturidade; }
}
