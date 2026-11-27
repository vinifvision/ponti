package com.ponti.api.dto;

import java.util.List;

// Uma página de resultados: os itens, o total (de todas as páginas), a página atual (começa em 0) e o tamanho.
public record PaginaAdmin<T>(List<T> itens, long total, int pagina, int tamanho) {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    public static <T> PaginaAdmin<T> de(List<T> todos, Integer pagina, Integer tamanho) {
        int t = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        int p = pagina == null || pagina < 0 ? 0 : pagina;
        int inicio = Math.min(p * t, todos.size());
        int fim = Math.min(inicio + t, todos.size());
        return new PaginaAdmin<>(List.copyOf(todos.subList(inicio, fim)), todos.size(), p, t);
    }
}
