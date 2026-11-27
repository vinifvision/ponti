package com.ponti.api.service;

import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Confere e-mail e senha.
@Service
public class LoginService {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";
    public static final String CONTA_BLOQUEADA =
            "Sua conta está bloqueada. Se achar que é um engano, fale com a equipe da Ponti.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock relogio;

    // Hash de uma senha qualquer, usado quando o e-mail não existe (ver autenticar)
    private final String hashFicticio;

    public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.relogio = relogio;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-comparacao");
    }

    // A resposta é a mesma para "e-mail não existe" e "senha errada", para não revelar
    // quais e-mails têm cadastro. Pelo mesmo motivo, mesmo sem usuário a senha é comparada
    // com um hash fictício: assim as duas situações demoram o mesmo tempo para responder.
    public Usuario autenticar(String email, String senha) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(email));
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFicticio);

        boolean senhaConfere = passwordEncoder.matches(senha, hash);
        if (usuario.isEmpty() || !senhaConfere) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }
        // Só avisa do bloqueio depois de a senha conferir, para não revelar nada a quem não é dono da conta
        if (usuario.get().estaBloqueado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CONTA_BLOQUEADA);
        }
        registrarAcesso(usuario.get());
        return usuario.get();
    }

    // Guarda o último acesso (usado pelo ranking de mentores). O Usuario decide se já passou
    // 1 hora desde o anterior; só então gravamos, para não escrever no banco a cada requisição.
    public void registrarAcesso(Usuario usuario) {
        if (usuario.registrarAcesso(Instant.now(relogio))) {
            usuarioRepository.save(usuario);
        }
    }
}
