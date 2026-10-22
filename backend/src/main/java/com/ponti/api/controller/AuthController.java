package com.ponti.api.controller;

import com.ponti.api.dto.CadastroMentorRequest;
import com.ponti.api.dto.CadastroStartupRequest;
import com.ponti.api.dto.LoginRequest;
import com.ponti.api.dto.SessaoResponse;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository,
                          StartupRepository startupRepository,
                          MentorRepository mentorRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Cria a startup e a conta de acesso dela de uma vez
    @PostMapping("/cadastro/startup")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SessaoResponse cadastrarStartup(@Valid @RequestBody CadastroStartupRequest req) {
        String login = normalizar(req.usuario());
        garantirLoginLivre(login);

        Startup startup = new Startup();
        startup.setNome(req.nome().trim());
        startup.setDescricao(req.descricao());
        startup.setSegmento(req.segmento());
        startup.setCidade(req.cidade());
        startup.setEstado(req.estado());
        startup.setEstagioMaturidade(req.estagioMaturidade());
        startup = startupRepository.save(startup);

        Usuario usuario = novoUsuario(login, req.senha(), TipoUsuario.STARTUP);
        usuario.setStartupId(startup.getId());
        usuario = usuarioRepository.save(usuario);

        return resposta(usuario, startup.getNome());
    }

    // Cria o mentor e a conta de acesso dele de uma vez
    @PostMapping("/cadastro/mentor")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SessaoResponse cadastrarMentor(@Valid @RequestBody CadastroMentorRequest req) {
        String login = normalizar(req.usuario());
        garantirLoginLivre(login);

        Mentor mentor = new Mentor();
        mentor.setNome(req.nome().trim());
        mentor.setBio(req.bio());
        mentor.setAreasAtuacao(req.areasAtuacao());
        mentor.setEcossistemas(req.ecossistemas());
        mentor.setValorSessao(req.valorSessao());
        mentor = mentorRepository.save(mentor);

        Usuario usuario = novoUsuario(login, req.senha(), TipoUsuario.MENTOR);
        usuario.setMentorId(mentor.getId());
        usuario = usuarioRepository.save(usuario);

        return resposta(usuario, mentor.getNome());
    }

    // Confere usuário e senha. A mensagem de erro é a mesma nos dois casos de propósito.
    @PostMapping("/login")
    public SessaoResponse entrar(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarioRepository.findByUsuario(normalizar(req.usuario()))
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuário ou senha inválidos"));
        return resposta(usuario, nomeDoPerfil(usuario));
    }

    private String normalizar(String usuario) {
        return usuario.trim().toLowerCase();
    }

    private void garantirLoginLivre(String login) {
        if (usuarioRepository.existsByUsuario(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse nome de usuário já está em uso");
        }
    }

    private Usuario novoUsuario(String login, String senha, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setUsuario(login);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setTipo(tipo);
        return usuario;
    }

    private String nomeDoPerfil(Usuario usuario) {
        if (usuario.getTipo() == TipoUsuario.STARTUP && usuario.getStartupId() != null) {
            return startupRepository.findById(usuario.getStartupId()).map(Startup::getNome).orElse(usuario.getUsuario());
        }
        if (usuario.getTipo() == TipoUsuario.MENTOR && usuario.getMentorId() != null) {
            return mentorRepository.findById(usuario.getMentorId()).map(Mentor::getNome).orElse(usuario.getUsuario());
        }
        return usuario.getUsuario();
    }

    private SessaoResponse resposta(Usuario usuario, String nome) {
        return new SessaoResponse(usuario.getId(), usuario.getUsuario(), usuario.getTipo(),
                usuario.getStartupId(), usuario.getMentorId(), nome);
    }
}
