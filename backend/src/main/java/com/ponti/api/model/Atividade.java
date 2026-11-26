package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;

// Item do plano que o mentor cria dentro de um acompanhamento ativo
@Entity
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long acompanhamentoId;

    @NotNull
    @Column(length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    // Horário de Recife, sem fuso (como o resto da API)
    @NotNull
    private LocalDateTime prazo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusAtividade status = StatusAtividade.PENDENTE;

    private Instant criadaEm = Instant.now();

    // A startup ainda precisa entregar (ou reenviar)?
    public boolean aEntregar() {
        return status == StatusAtividade.PENDENTE || status == StatusAtividade.DEVOLVIDA;
    }

    // Atrasada: o prazo passou e a startup ainda precisa entregar. É calculado na hora, nunca guardado,
    // porque uma atividade pode estar devolvida e atrasada ao mesmo tempo, e porque o atraso acontece
    // sozinho (sem ninguém fazer nada) quando o relógio passa do prazo.
    public boolean estaAtrasada(LocalDateTime agora) {
        return aEntregar() && prazo.isBefore(agora);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcompanhamentoId() { return acompanhamentoId; }
    public void setAcompanhamentoId(Long acompanhamentoId) { this.acompanhamentoId = acompanhamentoId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getPrazo() { return prazo; }
    public void setPrazo(LocalDateTime prazo) { this.prazo = prazo; }
    public StatusAtividade getStatus() { return status; }
    public void setStatus(StatusAtividade status) { this.status = status; }
    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }
}
