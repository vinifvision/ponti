package com.ponti.api.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Regras de "dono": cada startup e cada mentor só mexe no que é seu.
// Os controllers chamam estes métodos antes de alterar ou listar dados de alguém.
@Component
public class Acesso {

    public static final String SEM_PERMISSAO = "Você não tem permissão para fazer isso";

    // Quem está logado (as rotas protegidas já garantem que existe alguém)
    public UsuarioLogado atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioLogado logado)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar");
        }
        return logado;
    }

    // A startup logada precisa ser a startup informada
    public UsuarioLogado exigirStartup(Long startupId) {
        UsuarioLogado logado = atual();
        if (!logado.ehStartup(startupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SEM_PERMISSAO);
        }
        return logado;
    }

    // O mentor logado precisa ser o mentor informado
    public UsuarioLogado exigirMentor(Long mentorId) {
        UsuarioLogado logado = atual();
        if (!logado.ehMentor(mentorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SEM_PERMISSAO);
        }
        return logado;
    }

    // Quem está logado precisa ser um dos dois lados (a startup ou o mentor)
    public UsuarioLogado exigirParticipante(Long startupId, Long mentorId) {
        UsuarioLogado logado = atual();
        if (!logado.ehStartup(startupId) && !logado.ehMentor(mentorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SEM_PERMISSAO);
        }
        return logado;
    }
}
