package com.ponti.api.model;

// Situação do pagamento da mentoria. O pagamento é SIMULADO: nenhuma cobrança real é feita,
// mas o fluxo imita o de um gateway de verdade (ver GatewayPagamentoSimulado).
//   GRATUITA:    mentoria sem custo
//   AGUARDANDO:  Pix gerado, esperando a "confirmação do banco"
//   PAGO:        pagamento aprovado (Pix confirmado ou cartão aprovado)
//   REEMBOLSADO: estava pago e o pedido foi recusado ou cancelado
//   EXPIRADO:    o Pix não foi pago no prazo, ou o pedido foi cancelado antes do pagamento
public enum StatusPagamento {
    GRATUITA, AGUARDANDO, PAGO, REEMBOLSADO, EXPIRADO
}
