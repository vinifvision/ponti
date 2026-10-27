package com.ponti.api.model;

// PENDENTE: a startup pediu o horário e o mentor ainda não respondeu
// CONFIRMADO: o mentor confirmou e informou o link da reunião
// RECUSADO: o mentor recusou o pedido
// CANCELADO: alguém cancelou antes da sessão
public enum StatusAgendamento {
    PENDENTE, CONFIRMADO, RECUSADO, CANCELADO
}
