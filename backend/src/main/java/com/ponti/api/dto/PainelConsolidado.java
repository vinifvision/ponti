package com.ponti.api.dto;

// Painel no topo da lista: soma dos acompanhamentos ativos de quem está logado
public record PainelConsolidado(
        int acompanhamentosAtivos,
        int atividadesAprovadas,
        int atividadesTotal,
        // De 0 a 100: das atividades já entregues, quantas tiveram o primeiro envio dentro do prazo.
        // null enquanto não houve nenhuma entrega.
        Integer entregasNoPrazo,
        int metasCumpridas,
        int metasTotal) {
}
