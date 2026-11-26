package com.ponti.api.model;

// PENDENTE: a startup ainda não entregou
// ENTREGUE: a startup entregou e o mentor ainda não avaliou
// APROVADA: o mentor aprovou a última entrega
// DEVOLVIDA: o mentor pediu ajustes; a startup precisa reenviar
// "Atrasada" não é status: é calculado pelo prazo (ver Atividade.estaAtrasada)
public enum StatusAtividade {
    PENDENTE, ENTREGUE, APROVADA, DEVOLVIDA
}
