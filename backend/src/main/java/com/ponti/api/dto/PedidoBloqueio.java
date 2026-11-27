package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /admin/usuarios/{id}/bloquear. O motivo é obrigatório e vai para o registro de auditoria.
public record PedidoBloqueio(
        @NotBlank(message = "Informe o motivo do bloqueio")
        @Size(max = 500, message = "O motivo pode ter até 500 caracteres")
        String motivo) {
}
