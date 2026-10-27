package com.ponti.api.model;

// Pagamento SIMULADO (não existe cobrança de verdade nesta versão).
// GRATUITA: mentoria sem custo; PAGO: a startup "pagou" ao agendar;
// REEMBOLSADO: o pedido foi recusado ou cancelado e o valor "voltou".
public enum StatusPagamento {
    GRATUITA, PAGO, REEMBOLSADO
}
