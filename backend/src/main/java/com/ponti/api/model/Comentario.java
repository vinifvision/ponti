package com.ponti.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// Comentário na conversa de uma entrega (mentor e startup).
// O nome de quem escreveu não fica guardado aqui: vem do perfil na hora de mostrar,
// assim, se a pessoa muda o nome, a conversa acompanha.
@Entity
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long entregaId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoUsuario autor;

    @NotNull
    @Column(length = 1000)
    private String texto;

    @NotNull
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEntregaId() { return entregaId; }
    public void setEntregaId(Long entregaId) { this.entregaId = entregaId; }
    public TipoUsuario getAutor() { return autor; }
    public void setAutor(TipoUsuario autor) { this.autor = autor; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
