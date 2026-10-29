package com.ponti.api.dto;

import com.ponti.api.model.Startup;
import java.util.List;

// Uma startup na lista do mentor, com a compatibilidade (0 a 100) e os motivos escritos.
public record StartupRecomendada(Startup startup, int compatibilidade, List<String> motivos) {
}
