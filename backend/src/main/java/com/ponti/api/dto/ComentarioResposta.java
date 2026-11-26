package com.ponti.api.dto;

import com.ponti.api.model.TipoUsuario;
import java.time.LocalDateTime;

// Comentário na conversa de uma entrega
public record ComentarioResposta(Long id, TipoUsuario autor, String nomeAutor, String texto, LocalDateTime criadoEm) {
}
