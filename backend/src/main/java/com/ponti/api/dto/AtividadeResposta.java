package com.ponti.api.dto;

import com.ponti.api.model.StatusAtividade;
import java.time.LocalDateTime;
import java.util.List;

// Formato de "Atividade" em acompanhamento.models.ts, mais o campo "atrasada" calculado no servidor
public record AtividadeResposta(
        Long id,
        Long acompanhamentoId,
        String titulo,
        String descricao,
        LocalDateTime prazo,
        List<AnexoResposta> anexos,
        StatusAtividade status,
        boolean atrasada,
        List<EntregaResposta> entregas) {
}
