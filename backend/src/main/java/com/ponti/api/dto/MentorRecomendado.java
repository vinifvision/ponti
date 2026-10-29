package com.ponti.api.dto;

import java.util.List;

// Um mentor na lista da startup, com a compatibilidade (0 a 100) e os motivos escritos.
public record MentorRecomendado(MentorResposta mentor, int compatibilidade, List<String> motivos) {
}
