package com.ponti.api.service;

import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Confere e-mail e senha.
@Service
public class LoginService {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Hash de uma senha qualquer, usado quando o e-mail não existe (ver autenticar)
    private final String hashFicticio;

    public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
        return usuario.get();
    }
}
