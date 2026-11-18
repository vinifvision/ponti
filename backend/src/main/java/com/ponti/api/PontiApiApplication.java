package com.ponti.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// Liga as classes @ConfigurationProperties do projeto (ex.: RankingPropriedades)
@ConfigurationPropertiesScan
public class PontiApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PontiApiApplication.class, args);
    }
}
