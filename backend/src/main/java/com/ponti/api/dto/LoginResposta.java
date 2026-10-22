package com.ponti.api.dto;

import java.time.Instant;

// Resposta de POST /auth/login. O front guarda o token e o envia nas próximas chamadas
// no cabeçalho "Authorization: Bearer <token>".
public record LoginResposta(
        String token,
        String tipoToken,
        Instant expiraEm,
        UsuarioResumo usuario) {
}
