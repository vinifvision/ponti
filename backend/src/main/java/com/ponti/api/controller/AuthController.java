package com.ponti.api.controller;

import com.ponti.api.dto.CadastroMentor;
import com.ponti.api.dto.CadastroStartup;
import com.ponti.api.dto.CredenciaisLogin;
import com.ponti.api.dto.LoginResposta;
import com.ponti.api.dto.UsuarioResumo;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.CadastroService;
import com.ponti.api.service.JwtService;
import com.ponti.api.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final CadastroService cadastroService;
    private final LoginService loginService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(CadastroService cadastroService,
                          LoginService loginService,
                          JwtService jwtService,
                          UsuarioRepository usuarioRepository) {
        this.cadastroService = cadastroService;
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    // Cadastro de startup: cria o usuário e o perfil da startup juntos
    @PostMapping("/cadastro/startup")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResumo cadastrarStartup(@Valid @RequestBody CadastroStartup body) {
        return UsuarioResumo.de(cadastroService.cadastrarStartup(body));
    }

    // Cadastro de mentor: cria o usuário e o perfil do mentor juntos
    @PostMapping("/cadastro/mentor")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResumo cadastrarMentor(@Valid @RequestBody CadastroMentor body) {
        return UsuarioResumo.de(cadastroService.cadastrarMentor(body));
    }

    // Login: confere e-mail e senha e devolve um token JWT
    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody CredenciaisLogin body) {
        Usuario usuario = loginService.autenticar(body.email(), body.senha());
        JwtService.Token token = jwtService.gerar(usuario);
        return new LoginResposta(token.valor(), "Bearer", token.expiraEm(), UsuarioResumo.de(usuario));
    }

    // Quem está logado: valida o token do cabeçalho "Authorization: Bearer <token>"
    @GetMapping("/me")
    public UsuarioResumo eu(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIXO_BEARER)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token não informado");
        }
        Long usuarioId = jwtService.validar(authorization.substring(PREFIXO_BEARER.length()));

        // O token pode ser válido mas o usuário ter sido apagado depois
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResumo::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado"));
    }
}
