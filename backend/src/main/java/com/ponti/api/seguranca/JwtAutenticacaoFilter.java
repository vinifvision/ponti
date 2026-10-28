package com.ponti.api.seguranca;

import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

// Roda uma vez por requisição: se vier "Authorization: Bearer <token>" válido,
// registra quem é o usuário para os controllers. Token ausente ou inválido não barra aqui:
// quem barra é a configuração de segurança, só nas rotas que exigem login (resposta 401).
@Component
public class JwtAutenticacaoFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAutenticacaoFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            try {
                Long usuarioId = jwtService.validar(cabecalho.substring(PREFIXO.length()));
                // O usuário pode ter sido apagado depois que o token foi emitido
                usuarioRepository.findById(usuarioId).map(UsuarioLogado::de).ifPresent(logado -> {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                            logado, null, List.of(new SimpleGrantedAuthority("ROLE_" + logado.tipo().name())));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                });
            } catch (ResponseStatusException tokenInvalido) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
