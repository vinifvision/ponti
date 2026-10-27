package com.ponti.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
public class Mentor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @Column(length = 2000)
    private String bio;

    private String areasAtuacao;
    private String ecossistemas;

    // Tempo de experiência, em anos
    @Min(0) @Max(70)
    private Integer anosExperiencia;

    // Valor por sessão, definido pelo próprio mentor (0 = mentoria gratuita)
    private BigDecimal valorSessao;

    // Duração de cada sessão de mentoria, em minutos
    @Min(15) @Max(240)
    private Integer duracaoSessaoMinutos;

    // Endereço da foto de perfil
    @Size(max = 500)
    @Column(length = 500)
    private String fotoUrl;

    // Vídeo de apresentação (ex.: YouTube)
    @Size(max = 500)
    @Column(length = 500)
    private String videoUrl;

    @Size(max = 500)
    @Column(length = 500)
    private String linkedinUrl;

    // Contato direto. Só é gravado, nunca devolvido nas listagens:
    // a startup só recebe o WhatsApp depois que o convite é aceito (ver ConviteDetalhe).
    @Size(max = 30)
    @Column(length = 30)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String whatsapp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAreasAtuacao() { return areasAtuacao; }
    public void setAreasAtuacao(String areasAtuacao) { this.areasAtuacao = areasAtuacao; }
    public String getEcossistemas() { return ecossistemas; }
    public void setEcossistemas(String ecossistemas) { this.ecossistemas = ecossistemas; }
    public Integer getAnosExperiencia() { return anosExperiencia; }
    public void setAnosExperiencia(Integer anosExperiencia) { this.anosExperiencia = anosExperiencia; }
    public BigDecimal getValorSessao() { return valorSessao; }
    public void setValorSessao(BigDecimal valorSessao) { this.valorSessao = valorSessao; }
    public Integer getDuracaoSessaoMinutos() { return duracaoSessaoMinutos; }
    public void setDuracaoSessaoMinutos(Integer duracaoSessaoMinutos) { this.duracaoSessaoMinutos = duracaoSessaoMinutos; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
}
