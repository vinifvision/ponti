package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    // Valor por sessão, definido pelo próprio mentor
    private BigDecimal valorSessao;

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
    public BigDecimal getValorSessao() { return valorSessao; }
    public void setValorSessao(BigDecimal valorSessao) { this.valorSessao = valorSessao; }
}
