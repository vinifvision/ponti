package com.ponti.api.service;

import com.ponti.api.dto.AtividadeResposta;
import com.ponti.api.dto.AvaliacaoEntrega;
import com.ponti.api.dto.ComentarioResposta;
import com.ponti.api.dto.EntregaResposta;
import com.ponti.api.dto.NovaAtividade;
import com.ponti.api.dto.NovaEntrega;
import com.ponti.api.dto.NovoComentario;
import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Atividade;
import com.ponti.api.model.Comentario;
import com.ponti.api.model.Entrega;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.StatusAtividade;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AtividadeRepository;
import com.ponti.api.repository.ComentarioRepository;
import com.ponti.api.repository.EntregaRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Atividades e entregas de um acompanhamento. Permissões:
//  - ver: só o mentor e a startup do acompanhamento (para os outros, 404);
//  - criar atividade, aprovar e devolver: só o mentor;
//  - entregar e desfazer: só a startup;
//  - comentar: os dois lados;
//  - qualquer mudança: só com o acompanhamento ativo.
@Service
public class AtividadeService {

    public static final String NAO_ENCONTRADA = "Atividade não encontrada.";

    private final AtividadeRepository repository;
    private final EntregaRepository entregaRepository;
    private final ComentarioRepository comentarioRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;
    private final AcompanhamentoService acompanhamentoService;
    private final Acesso acesso;
    private final Clock relogio;

    public AtividadeService(AtividadeRepository repository,
                            EntregaRepository entregaRepository,
                            ComentarioRepository comentarioRepository,
                            AcompanhamentoRepository acompanhamentoRepository,
                            MentorRepository mentorRepository,
                            StartupRepository startupRepository,
                            AcompanhamentoService acompanhamentoService,
                            Acesso acesso,
                            Clock relogio) {
        this.repository = repository;
        this.entregaRepository = entregaRepository;
        this.comentarioRepository = comentarioRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
        this.acompanhamentoService = acompanhamentoService;
        this.acesso = acesso;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<AtividadeResposta> listar(Long acompanhamentoId) {
        UsuarioLogado logado = acesso.atual();
        acompanhamentoService.doParticipante(acompanhamentoId, logado);
        LocalDateTime agora = agora();
        return repository.findByAcompanhamentoIdOrderByPrazoAscIdAsc(acompanhamentoId).stream()
                .map(a -> resposta(a, agora)).toList();
    }

    @Transactional
    public AtividadeResposta criar(Long acompanhamentoId, NovaAtividade nova) {
        UsuarioLogado logado = acesso.atual();
        Acompanhamento ac = acompanhamentoService.doParticipante(acompanhamentoId, logado);
        exigir(logado, TipoUsuario.MENTOR);
        exigirAtivo(ac);
        if (!nova.prazo().isAfter(agora())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um prazo que ainda não passou.");
        }
        Atividade a = new Atividade();
        a.setAcompanhamentoId(acompanhamentoId);
        a.setTitulo(nova.titulo().trim());
        a.setDescricao(nova.descricao() == null ? "" : nova.descricao().trim());
        a.setPrazo(nova.prazo());
        return resposta(repository.save(a), agora());
    }

    @Transactional
    public AtividadeResposta entregar(Long atividadeId, NovaEntrega nova) {
        UsuarioLogado logado = acesso.atual();
        Atividade a = daAtividadeAtiva(atividadeId, logado);
        exigir(logado, TipoUsuario.STARTUP);
        if (!a.aEntregar()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta atividade não aceita uma nova entrega agora.");
        }
        String texto = limpar(nova.texto());
        String link = limpar(nova.link());
        if (texto == null && link == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva um texto, informe um link ou anexe um arquivo.");
        }
        // Só links da web: bloqueia endereços como "javascript:", que poderiam rodar código no navegador de quem clica
        if (link != null && !link.matches("(?i)^https?://\\S+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um link que comece com http:// ou https://.");
        }
        Entrega e = new Entrega();
        e.setAtividadeId(atividadeId);
        e.setTexto(texto == null ? "" : texto);
        e.setLink(link);
        e.setEnviadaEm(agora());
        entregaRepository.save(e);
        a.setStatus(StatusAtividade.ENTREGUE);
        return resposta(a, agora());
    }

    // Desfaz o último envio enquanto o mentor não avaliou. Se havia uma entrega anterior (já devolvida),
    // a atividade volta a "devolvida"; senão, a "pendente".
    @Transactional
    public AtividadeResposta desfazerEntrega(Long atividadeId) {
        UsuarioLogado logado = acesso.atual();
        Atividade a = daAtividadeAtiva(atividadeId, logado);
        exigir(logado, TipoUsuario.STARTUP);
        if (a.getStatus() != StatusAtividade.ENTREGUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só dá para desfazer um envio que o mentor ainda não avaliou.");
        }
        List<Entrega> entregas = entregaRepository.findByAtividadeIdOrderByIdAsc(atividadeId);
        Entrega ultima = entregas.get(entregas.size() - 1);
        comentarioRepository.deleteByEntregaId(ultima.getId());
        entregaRepository.delete(ultima);
        a.setStatus(entregas.size() > 1 ? StatusAtividade.DEVOLVIDA : StatusAtividade.PENDENTE);
        return resposta(a, agora());
    }

    @Transactional
    public AtividadeResposta aprovar(Long atividadeId, AvaliacaoEntrega avaliacao) {
        return avaliar(atividadeId, StatusAtividade.APROVADA, avaliacao == null ? null : limpar(avaliacao.feedback()));
    }

    @Transactional
    public AtividadeResposta devolver(Long atividadeId, AvaliacaoEntrega avaliacao) {
        String feedback = avaliacao == null ? null : limpar(avaliacao.feedback());
        if (feedback == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explique o que precisa ser ajustado.");
        }
        return avaliar(atividadeId, StatusAtividade.DEVOLVIDA, feedback);
    }

    private AtividadeResposta avaliar(Long atividadeId, StatusAtividade novo, String feedback) {
        UsuarioLogado logado = acesso.atual();
        Atividade a = daAtividadeAtiva(atividadeId, logado);
        exigir(logado, TipoUsuario.MENTOR);
        if (a.getStatus() != StatusAtividade.ENTREGUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há entrega para avaliar.");
        }
        List<Entrega> entregas = entregaRepository.findByAtividadeIdOrderByIdAsc(atividadeId);
        entregas.get(entregas.size() - 1).setFeedback(feedback);
        a.setStatus(novo);
        return resposta(a, agora());
    }

    // Comentário numa entrega (os dois lados podem comentar, com o acompanhamento ativo)
    @Transactional
    public AtividadeResposta comentar(Long atividadeId, Long entregaId, NovoComentario novo) {
        UsuarioLogado logado = acesso.atual();
        Atividade a = daAtividadeAtiva(atividadeId, logado);
        Entrega entrega = entregaRepository.findById(entregaId)
                .filter(e -> e.getAtividadeId().equals(atividadeId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega não encontrada."));
        Comentario c = new Comentario();
        c.setEntregaId(entrega.getId());
        c.setAutor(logado.tipo());
        c.setTexto(novo.texto().trim());
        c.setCriadoEm(agora());
        comentarioRepository.save(c);
        return resposta(a, agora());
    }

    // ----- apoio -----

    // Busca a atividade e confere se quem está logado participa do acompanhamento dela (senão: 404)
    Atividade daParticipante(Long atividadeId, UsuarioLogado logado) {
        Atividade a = repository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_ENCONTRADA));
        try {
            acompanhamentoService.doParticipante(a.getAcompanhamentoId(), logado);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_ENCONTRADA);
        }
        return a;
    }

    Atividade daAtividadeAtiva(Long atividadeId, UsuarioLogado logado) {
        Atividade a = daParticipante(atividadeId, logado);
        exigirAtivo(acompanhamentoService.doParticipante(a.getAcompanhamentoId(), logado));
        return a;
    }

    static void exigirAtivo(Acompanhamento ac) {
        if (ac.getStatus() != StatusAcompanhamento.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este acompanhamento não está ativo.");
        }
    }

    static void exigir(UsuarioLogado logado, TipoUsuario papel) {
        if (logado.tipo() != papel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO);
        }
    }

    AtividadeResposta resposta(Atividade a, LocalDateTime agora) {
        Nomes nomes = nomes(a.getAcompanhamentoId());
        List<EntregaResposta> entregas = entregaRepository.findByAtividadeIdOrderByIdAsc(a.getId()).stream()
                .map(e -> new EntregaResposta(e.getId(), e.getTexto(), e.getLink(), List.of(), e.getEnviadaEm(),
                        e.getFeedback(), comentarios(e.getId(), nomes)))
                .toList();
        return new AtividadeResposta(a.getId(), a.getAcompanhamentoId(), a.getTitulo(), a.getDescricao(), a.getPrazo(),
                List.of(), a.getStatus(), a.estaAtrasada(agora), entregas);
    }

    private List<ComentarioResposta> comentarios(Long entregaId, Nomes nomes) {
        return comentarioRepository.findByEntregaIdOrderByIdAsc(entregaId).stream()
                .map(c -> new ComentarioResposta(c.getId(), c.getAutor(),
                        c.getAutor() == TipoUsuario.MENTOR ? nomes.mentor() : nomes.startup(),
                        c.getTexto(), c.getCriadoEm()))
                .toList();
    }

    // Nome atual do mentor e da startup do acompanhamento (para mostrar quem escreveu cada comentário)
    private record Nomes(String mentor, String startup) {
    }

    private Nomes nomes(Long acompanhamentoId) {
        Acompanhamento ac = acompanhamentoRepository.findById(acompanhamentoId).orElseThrow();
        String mentor = mentorRepository.findById(ac.getMentorId()).map(m -> m.getNome()).orElse("Mentor");
        String startup = startupRepository.findById(ac.getStartupId()).map(s -> s.getNome()).orElse("Startup");
        return new Nomes(mentor, startup);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
