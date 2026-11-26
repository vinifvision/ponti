package com.ponti.api.dto;

import com.ponti.api.model.StatusAcompanhamento;
import java.time.LocalDate;

// Card da lista e cabeçalho da página do acompanhamento (mesmo formato de acompanhamento.models.ts).
// Os números são contados a partir das atividades e metas guardadas no banco.
public record ResumoAcompanhamento(
        Long id,
        StatusAcompanhamento status,
        String objetivo,
        LocalDate dataInicio,
        LocalDate dataFim,
        ParceiroAcompanhamento parceiro,
        int atividadesAprovadas,
        int atividadesTotal,
        int metasCumpridas,
        int metasTotal,
        // Só em propostas: quantas metas o mentor sugeriu (nas outras situações vem null)
        Integer metasSugeridas,
        LocalDate proximoPrazo,
        int atividadesAtrasadas,
        // Startup: atividades a entregar. Mentor: entregas a revisar.
        int pendencias) {
}
