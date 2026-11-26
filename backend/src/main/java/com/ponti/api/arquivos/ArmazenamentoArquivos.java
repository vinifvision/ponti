package com.ponti.api.arquivos;

import java.util.Collection;

// Onde os arquivos anexados ficam guardados.
//  - Em produção: ArmazenamentoSupabase (bucket privado no Supabase Storage).
//  - No PC e nos testes: ArmazenamentoLocal (em memória, some quando a API reinicia, igual ao banco H2).
// O resto do código só conhece esta interface, então trocar de lugar não muda as regras.
public interface ArmazenamentoArquivos {

    // Guarda o conteúdo no caminho informado (ex.: "acompanhamentos/12/3f2a...pdf")
    void salvar(String caminho, byte[] conteudo, String tipoConteudo);

    // Link temporário para baixar o arquivo, já com o nome que a pessoa vai ver ao salvar
    String linkTemporario(String caminho, String nomeParaBaixar);

    // Apaga os arquivos (ignora os que não existem)
    void apagar(Collection<String> caminhos);
}
