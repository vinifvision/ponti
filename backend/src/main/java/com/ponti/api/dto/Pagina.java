package com.ponti.api.dto;

import java.util.List;
import java.util.function.Function;

// Uma página de resultados: os itens desta página e o total, para a tela montar a paginação.
//   { "itens": [...], "pagina": 0, "tamanho": 12, "totalItens": 37, "totalPaginas": 4 }
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {

    // Corta a lista inteira (já filtrada e ordenada) na página pedida. Página além do fim volta vazia.
    public static <T> Pagina<T> de(List<T> todos, int pagina, int tamanho) {
        int inicio = (int) Math.min((long) pagina * tamanho, todos.size());
        int fim = Math.min(inicio + tamanho, todos.size());
        int totalPaginas = (todos.size() + tamanho - 1) / tamanho;
        return new Pagina<>(List.copyOf(todos.subList(inicio, fim)), pagina, tamanho, todos.size(), totalPaginas);
    }

    // A mesma página com os itens transformados (os totais não mudam)
    public <R> Pagina<R> mapear(Function<T, R> transformar) {
        return new Pagina<>(itens.stream().map(transformar).toList(), pagina, tamanho, totalItens, totalPaginas);
    }
}
