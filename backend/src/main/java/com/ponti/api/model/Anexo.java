package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

// Arquivo anexado: material de apoio do mentor (atividadeId) ou arquivo da entrega da startup (entregaId).
// O conteúdo fica no armazenamento de arquivos (Supabase Storage); aqui fica só a "ficha" do arquivo.
@Entity
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long acompanhamentoId;

    // Um dos dois vem preenchido
    private Long atividadeId;
    private Long entregaId;

    // Nome original, só para mostrar e para o download
    @NotNull
    @Column(length = 255)
    private String nome;

    private long tamanho;

    @NotNull
    @Column(length = 120)
    private String tipoConteudo;

    // Onde o arquivo está no armazenamento. Gerado pelo servidor (nunca usa o nome enviado).
    @NotNull
    @Column(length = 300)
    private String caminho;

    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcompanhamentoId() { return acompanhamentoId; }
    public void setAcompanhamentoId(Long acompanhamentoId) { this.acompanhamentoId = acompanhamentoId; }
    public Long getAtividadeId() { return atividadeId; }
    public void setAtividadeId(Long atividadeId) { this.atividadeId = atividadeId; }
    public Long getEntregaId() { return entregaId; }
    public void setEntregaId(Long entregaId) { this.entregaId = entregaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public long getTamanho() { return tamanho; }
    public void setTamanho(long tamanho) { this.tamanho = tamanho; }
    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }
    public String getCaminho() { return caminho; }
    public void setCaminho(String caminho) { this.caminho = caminho; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
