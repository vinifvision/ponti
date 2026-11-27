package com.ponti.api.model;

import jakarta.persistence.*;
import java.time.Instant;

// Registro de auditoria: quem fez o quê, com quem, quando e por quê.
// Só é criado (nunca editado nem apagado pela API), para servir de histórico confiável.
// Guarda um "retrato" do nome e do e-mail do autor e do alvo, para o registro continuar legível
// mesmo se a conta for excluída depois.
@Entity
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long autorUsuarioId;

    @Column(nullable = false)
    private String autorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    private Long alvoUsuarioId;

    private String alvoDescricao;

    @Column(length = 500)
    private String motivo;

    @Column(nullable = false)
    private Instant criadoEm;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Usuario autor, AcaoAuditoria acao, Long alvoUsuarioId, String alvoDescricao,
                             String motivo, Instant criadoEm) {
        this.autorUsuarioId = autor.getId();
        this.autorEmail = autor.getEmail();
        this.acao = acao;
        this.alvoUsuarioId = alvoUsuarioId;
        this.alvoDescricao = alvoDescricao;
        this.motivo = motivo;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }
    public Long getAutorUsuarioId() { return autorUsuarioId; }
    public String getAutorEmail() { return autorEmail; }
    public AcaoAuditoria getAcao() { return acao; }
    public Long getAlvoUsuarioId() { return alvoUsuarioId; }
    public String getAlvoDescricao() { return alvoDescricao; }
    public String getMotivo() { return motivo; }
    public Instant getCriadoEm() { return criadoEm; }
}
