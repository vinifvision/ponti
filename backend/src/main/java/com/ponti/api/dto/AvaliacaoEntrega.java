package com.ponti.api.dto;

import jakarta.validation.constraints.Size;

// Resposta do mentor a uma entrega (aprovar: opcional; devolver: obrigatório)
public record AvaliacaoEntrega(
        @Size(max = 2000, message = "O feedback pode ter até 2000 caracteres.")
        String feedback) {
}
