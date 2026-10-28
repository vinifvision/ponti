package com.ponti.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Liga as tarefas agendadas (ex.: o "banco" simulado que confirma os Pix)
@Configuration
@EnableScheduling
public class AgendadorConfig {
}
