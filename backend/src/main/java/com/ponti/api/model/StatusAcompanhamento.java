package com.ponti.api.model;

// PROPOSTO: o mentor propôs e a startup ainda não respondeu
// ATIVO: a startup aceitou; o mentor cria atividades e a startup entrega
// RECUSADO: a startup recusou a proposta (fica guardado como histórico, mas some da lista)
// ENCERRADO: o mentor encerrou; tudo vira somente leitura
public enum StatusAcompanhamento {
    PROPOSTO, ATIVO, RECUSADO, ENCERRADO
}
