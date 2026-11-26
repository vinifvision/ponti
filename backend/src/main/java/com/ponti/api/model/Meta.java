package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

// Meta do acompanhamento. O mentor sugere na proposta e depois marca como cumprida.
@Entity
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long acompanhamentoId;

    @NotNull
    @Column(length = 200)
    private String titulo;

    // Posição na lista (a ordem em que o mentor escreveu)
    private int ordem;

    private boolean cumprida;

    private Instant cumpridaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcompanhamentoId() { return acompanhamentoId; }
    public void setAcompanhamentoId(Long acompanhamentoId) { this.acompanhamentoId = acompanhamentoId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public boolean isCumprida() { return cumprida; }
    public void setCumprida(boolean cumprida) { this.cumprida = cumprida; }
    public Instant getCumpridaEm() { return cumpridaEm; }
    public void setCumpridaEm(Instant cumpridaEm) { this.cumpridaEm = cumpridaEm; }
}
