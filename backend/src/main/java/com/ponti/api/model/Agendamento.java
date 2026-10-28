package com.ponti.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

// Sessão de mentoria marcada pela startup num horário livre do mentor (fluxo parecido com o do Preply):
// a startup escolhe o horário e paga (simulado); o mentor confirma com o link do Google Meet ou recusa.
@Entity
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private Long mentorId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    // Preço do mentor no momento do agendamento (se ele mudar o preço depois, este não muda)
    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento statusPagamento;

    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodoPagamento;

    // Link da reunião (ex.: Google Meet), informado pelo mentor ao confirmar
    @Column(length = 500)
    private String linkReuniao;

    // Recado da startup para o mentor: o que quer tratar na sessão
    @Column(length = 1000)
    private String mensagem;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    // ---- Pagamento (simulado) ----

    // Código da transação, mostrado no comprovante (ex.: PNT-7K2M9QX4)
    @Column(length = 20)
    private String codigoTransacao;

    // Código Pix "copia e cola" (padrão BR Code), enquanto o pagamento está AGUARDANDO
    @Column(length = 500)
    private String pixCopiaECola;

    // Até quando o Pix pode ser pago
    private LocalDateTime pagamentoExpiraEm;

    private LocalDateTime pagoEm;

    // Do cartão guardamos só a bandeira e os 4 últimos dígitos (nunca o número inteiro nem o CVV)
    @Column(length = 4)
    private String cartaoFinal;

    @Column(length = 20)
    private String cartaoBandeira;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(StatusPagamento statusPagamento) { this.statusPagamento = statusPagamento; }
    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public String getLinkReuniao() { return linkReuniao; }
    public void setLinkReuniao(String linkReuniao) { this.linkReuniao = linkReuniao; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public String getCodigoTransacao() { return codigoTransacao; }
    public void setCodigoTransacao(String codigoTransacao) { this.codigoTransacao = codigoTransacao; }
    public String getPixCopiaECola() { return pixCopiaECola; }
    public void setPixCopiaECola(String pixCopiaECola) { this.pixCopiaECola = pixCopiaECola; }
    public LocalDateTime getPagamentoExpiraEm() { return pagamentoExpiraEm; }
    public void setPagamentoExpiraEm(LocalDateTime pagamentoExpiraEm) { this.pagamentoExpiraEm = pagamentoExpiraEm; }
    public LocalDateTime getPagoEm() { return pagoEm; }
    public void setPagoEm(LocalDateTime pagoEm) { this.pagoEm = pagoEm; }
    public String getCartaoFinal() { return cartaoFinal; }
    public void setCartaoFinal(String cartaoFinal) { this.cartaoFinal = cartaoFinal; }
    public String getCartaoBandeira() { return cartaoBandeira; }
    public void setCartaoBandeira(String cartaoBandeira) { this.cartaoBandeira = cartaoBandeira; }
}
