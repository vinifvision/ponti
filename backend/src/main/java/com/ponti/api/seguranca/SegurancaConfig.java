package com.ponti.api.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ponti.api.erro.Problemas;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Segurança da API:
//  - sem sessão: cada requisição se identifica pelo token JWT (cabeçalho Authorization);
//  - rotas públicas: login, cadastro, /auth/me (que confere o token sozinho) e a documentação;
//  - todo o resto exige login (401). As regras de dono ficam na classe Acesso (403).
@Configuration
public class SegurancaConfig {

    private final JwtAutenticacaoFilter jwtFiltro;
    private final ObjectMapper objectMapper;

    @Value("${spring.h2.console.enabled:false}")
    private boolean consoleH2;

    @Value("${ponti.cors.origens:http://localhost:4200,https://*.app.github.dev}")
    private List<String> origens;

    public SegurancaConfig(JwtAutenticacaoFilter jwtFiltro, ObjectMapper objectMapper) {
        this.jwtFiltro = jwtFiltro;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sem cookies de sessão: CSRF não se aplica
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(rotas -> {
                    rotas.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    rotas.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/cadastro/startup", "/auth/cadastro/mentor").permitAll();
                    rotas.requestMatchers(HttpMethod.GET, "/auth/me", "/temas").permitAll();
                    rotas.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll();
                    if (consoleH2) {
                        rotas.requestMatchers("/h2-console/**").permitAll();
                    }
                    rotas.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escrever(res, HttpStatus.UNAUTHORIZED, "Faça login para continuar", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) ->
                                escrever(res, HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO, req.getRequestURI())))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);

        if (consoleH2) {
            // O console do H2 (só em desenvolvimento) roda dentro de um <iframe>
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        }
        return http.build();
    }

    // Libera o front em Angular (localhost:4200 e Codespaces) para chamar a API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(origens);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }

    // 401 e 403 no mesmo formato de erro do resto da API (Problem Details)
    private void escrever(HttpServletResponse res, HttpStatus status, String mensagem, String rota) throws IOException {
        ProblemDetail problema = Problemas.criar(status, mensagem, rota);
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), problema);
    }
}
