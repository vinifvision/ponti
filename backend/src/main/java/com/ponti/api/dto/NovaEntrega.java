package com.ponti.api.dto;

import jakarta.validation.constraints.Size;

public record NovaEntrega(
        @Size(max = 4000, message = "O texto pode ter até 4000 caracteres.")
        String texto,
        @Size(max = 500, message = "O link pode ter até 500 caracteres.")
        String link) {
}
