package com.ponti.api.dto;

import jakarta.validation.constraints.Size;

// Corpo (opcional) de POST /admin/usuarios/{id}/desbloquear
public record PedidoDesbloqueio(
        @Size(max = 500, message = "O motivo pode ter até 500 caracteres")
        String motivo) {
}
