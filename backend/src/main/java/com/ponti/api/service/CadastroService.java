package com.ponti.api.service;

import com.ponti.api.dto.CadastroMentor;
import com.ponti.api.dto.CadastroStartup;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Cria o perfil (startup ou mentor) e o usuário dono dele.
// @Transactional: se algo falhar no meio, nada é gravado (não sobra perfil sem usuário).
@Service
public class CadastroService {

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final PasswordEncoder passwordEncoder;

    public CadastroService(UsuarioRepository usuarioRepository,
                           StartupRepository startupRepository,
                           MentorRepository mentorRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarStartup(CadastroStartup dados) {
        Usuario usuario = novoUsuario(dados.email(), dados.senha(), TipoUsuario.STARTUP);

        Startup startup = new Startup();
        startup.setNome(dados.nome());
        startup.setSegmento(dados.areaAtuacao());
        startup.setDescricao(dados.sobre());
        // estagioMaturidade fica vazio: é preenchido depois, pelo formulário de maturidade

        usuario.setStartupId(startupRepository.save(startup).getId());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cadastrarMentor(CadastroMentor dados) {
        Usuario usuario = novoUsuario(dados.email(), dados.senha(), TipoUsuario.MENTOR);

        Mentor mentor = new Mentor();
        mentor.setNome(dados.nome());
        mentor.setAreasAtuacao(dados.areaAtuacao());
        mentor.setAnosExperiencia(dados.anosExperiencia());
        mentor.setBio(dados.sobre());

        usuario.setMentorId(mentorRepository.save(mentor).getId());
        return usuarioRepository.save(usuario);
    }

    private Usuario novoUsuario(String email, String senha, TipoUsuario tipo) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setTipo(tipo);
        return usuario;
    }
}
