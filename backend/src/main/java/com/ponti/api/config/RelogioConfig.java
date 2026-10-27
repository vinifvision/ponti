package com.ponti.api.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {

    // Horário oficial da plataforma (Recife). Ter o relógio como bean facilita testar regras de data.
    public static final ZoneId FUSO = ZoneId.of("America/Recife");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO);
    }
}
